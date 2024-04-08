package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.*;
import cn.gzten.jabu.pojo.PendingInjectMethodWithDependency;
import cn.gzten.jabu.pojo.PendingInjectionField;
import cn.gzten.jabu.pojo.PendingPropField;
import cn.gzten.jabu.pojo.SimClassInfo;
import cn.gzten.jabu.util.JabuProcessorUtil;
import cn.gzten.jabu.util.JabuUtils;
import com.squareup.javapoet.*;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BeanProcessor {
    private static final List<PendingInjectionField> pendingInjectionFields = new CopyOnWriteArrayList<>();
    private static final List<PendingPropField> pendingPropFields = new CopyOnWriteArrayList<>();

    private static final List<Map.Entry<ExecutableElement, String>> preDestroys = new CopyOnWriteArrayList<>();

    private static final List<Map.Entry<ExecutableElement, String>> postConstructs = new CopyOnWriteArrayList<>();
    private static final Map<TypeName, Set<String>> beans = new ConcurrentHashMap<>();

    private static final List<PendingInjectMethodWithDependency> pendingInjectMethodWithDependencies = new CopyOnWriteArrayList<>();

    public static <T  extends Annotation> void process(RoundEnvironment roundEnv,
                                                       TypeSpec.Builder classSpecBuilder,
                                                       MethodSpec.Builder initMethodBuilder,
                                                       MethodSpec.Builder getBeanMethodBuilder,
                                                       Class<T> annotationType) {
        var elements = roundEnv.getElementsAnnotatedWith(annotationType);
        if (elements.isEmpty()) return;

        for (var element : elements) {
            T beanAnnotation = element.getAnnotation(annotationType);
            // When it is a class, process, skip those case that annotated at method level
            if (element instanceof TypeElement) {
                doBeanBasics(annotationType, beanAnnotation, (TypeElement)element, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
            }
        }
    }

    public static <T  extends Annotation> void doBeanBasics(Class<T> annotationType,
                                                            T beanAnnotation,
                                                            TypeElement element,
                                                            TypeSpec.Builder classSpecBuilder,
                                                            MethodSpec.Builder initMethodBuilder,
                                                            MethodSpec.Builder getBeanMethodBuilder) {
        // HasBean is not a bean, so it can be used along with other Bean annotations, like @Bean/@Service/@Controller
        if (HasBean.class.equals(annotationType)) {
            for (Element el : element.getEnclosedElements()){
                if (!(el instanceof ExecutableElement)) continue;

                var beanMethodAnnotation = el.getAnnotation(Bean.class);
                if (beanMethodAnnotation != null) {
                    preprocessMethodBean(element, (ExecutableElement) el, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
                }
            }
            return;
        }

        String classFullName = element.toString();
        System.out.println("Pre-processing bean: " + classFullName);
        var supers = new LinkedList<TypeName>();
        // prepare the supers, for interfaces
        element.getInterfaces().forEach(m -> {
            if (JabuProcessorUtil.typeMirrorIsType(m, Serializable.class)) return;
            if (m.toString().startsWith("java.lang")) return;
            supers.add(TypeName.get(m));
        });
        // prepare the supers, for its super class
        if (!element.getSuperclass().toString().startsWith("java.lang")) {
            supers.add(TypeName.get(element.getSuperclass()));
        }

        var classInfo = SimClassInfo.from(classFullName, element);
        Method nameMethod;
        try {
            nameMethod = annotationType.getDeclaredMethod("name");
        } catch (NoSuchMethodException e) {
            nameMethod = null;
        }

        // For @Bean, @Service and @Controller, all has "name", but for @HasBean, no, it is just a sign.
        if (nameMethod != null) {
            AtomicReference<String> nameRef = new AtomicReference<>();
            try {
                var nameInAnnotation = nameMethod.invoke(beanAnnotation).toString();

                // If the Controller defined customized name, use it, if no, use camel case of the class name as the bean name
                if (StringUtil.isNotBlank(nameInAnnotation)) {
                    nameRef.set(nameInAnnotation);
                } else {
                    nameRef.set(classInfo.getClassNameCamelCase());
                }
                cacheBeanName(classInfo.getTypeName(), nameRef.get(), false);
                supers.forEach(sp -> cacheBeanName(sp, nameRef.get(), true));

            } catch (InvocationTargetException|IllegalAccessException ex) {
                ex.printStackTrace();
                JabuProcessorUtil.fail("Fatal error at doBeanBasics!");
            }

            addFillBeanCodeBlock(classInfo, nameRef.get(), classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);

            /**
             * Main loops for fields and methods
             */
            mainLoopForFieldsAndMethods(element, nameRef.get());
        }
    }

    public static void preprocessMethodBean(TypeElement methodBelongsToClass,
                                            ExecutableElement element,
                                            TypeSpec.Builder classSpecBuilder,
                                            MethodSpec.Builder initMethodBuilder,
                                            MethodSpec.Builder getBeanMethodBuilder) {
        System.out.println("Method Bean found: " + element);
        var params = element.getParameters();
        if (!element.getModifiers().contains(Modifier.STATIC)) {
            JabuProcessorUtil.fail("@Bean method has to be static");
        }
        var returnType = element.getReturnType();
        var beanAnnotation = element.getAnnotation(Bean.class);
        if (beanAnnotation == null) JabuProcessorUtil.fail("Not a @Bean method!");

        var nameRef = new AtomicReference<String>();
        if (StringUtil.isNotBlank(beanAnnotation.name())) {
            nameRef.set(beanAnnotation.name());
        } else {
            nameRef.set(SimClassInfo.convertTypeNameToSimpleCamelCase(TypeName.get(returnType)));
        }

        // Cache all the bean name for future processing.
        cacheBeanName(TypeName.get(returnType), nameRef.get(), false);
        try {
            var clz = Class.forName(returnType.toString());
            for(var in : clz.getInterfaces()) {
                if (in.equals(Serializable.class)) continue;
                if (in.getCanonicalName().startsWith("java.lang")) continue;

                cacheBeanName(TypeName.get(in), nameRef.get(), false);
            }
            if (clz.getSuperclass() != null && !clz.getSuperclass().getCanonicalName().startsWith("java.lang")) {
                cacheBeanName(TypeName.get(clz.getSuperclass()), nameRef.get(), false);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot load class during compile time: " + returnType);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }

        // Very simple no dependency
        if (params.isEmpty()) {
            addFillBeanCodeBlockForMethodBeanWithEmptyParams(methodBelongsToClass.asType(), returnType, nameRef.get(),
                    element.getSimpleName().toString(),
                    classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
        } else {
            var o = new PendingInjectMethodWithDependency();
            o.returnType = TypeName.get(returnType);
            o.belongToClassType = TypeName.get(methodBelongsToClass.asType());
            o.methodName = element.getSimpleName().toString();
            o.methodBeanName = nameRef.get();
            for (var param : params) {
                var methodParam = new PendingInjectMethodWithDependency.Param();
                methodParam.paramType = TypeName.get(param.asType());
                methodParam.paramName = param.getSimpleName().toString();
                var qualifier = param.getAnnotation(Qualifier.class);
                if (qualifier != null) {
                    if (StringUtil.isNotBlank(qualifier.value())) {
                        methodParam.qualifier = qualifier.value();
                    } else {
                        methodParam.qualifier = methodParam.paramName;
                    }
                }
                o.params.add(methodParam);
            }
            pendingInjectMethodWithDependencies.add(o);
        }
    }

    public static void preprocessPreDestroy(ExecutableElement element, String beanName) {
        var annotation = element.getAnnotation(PreDestroy.class);
        if (annotation == null) return;
        preDestroys.add(Map.entry(element, beanName));
    }

    public static void preprocessPostConstruct(ExecutableElement element, String beanName) {
        var annotation = element.getAnnotation(PostConstruct.class);
        if (annotation == null) return;

        postConstructs.add(Map.entry(element, beanName));
    }

    private static void addFillBeanCodeBlockForMethodBeanWithEmptyParams(TypeMirror clazzType,
                                                                         TypeMirror returnType,
                                                                         String beanName,
                                                                         String methodName,
                                                                         TypeSpec.Builder classSpecBuilder,
                                                                         MethodSpec.Builder initMethodBuilder,
                                                                         MethodSpec.Builder getBeanMethodBuilder) {
        // $L(for Literals), $S(for Strings), $T(for Types), $N(for Names), as placeholders
        try {
            FieldSpec fieldSpec = FieldSpec.builder(TypeName.get(returnType), beanName)
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            getBeanMethodBuilder.addCode(CodeBlock.of("if($S.equals(beanName)) return $N;\n", beanName, beanName));
            initMethodBuilder.addStatement("$N = $T.$N()", beanName, TypeName.get(clazzType), methodName);

            initMethodBuilder.addStatement("fillBean($S, $N)", beanName, beanName);

            classSpecBuilder.addField(fieldSpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void addFillBeanCodeBlock(SimClassInfo classInfo,
                                            String beanName,
                                            TypeSpec.Builder classSpecBuilder,
                                            MethodSpec.Builder initMethodBuilder,
                                            MethodSpec.Builder getBeanMethodBuilder) {
        // $L(for Literals), $S(for Strings), $T(for Types), $N(for Names), as placeholders
        try {
            FieldSpec fieldSpec = FieldSpec.builder(classInfo.getTypeName(), beanName)
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            getBeanMethodBuilder.addCode(CodeBlock.of("if($S.equals(beanName)) return $N;\n", beanName, beanName));
            initMethodBuilder.addStatement("$N = new $T()", beanName, classInfo.getTypeName());

            initMethodBuilder.addStatement("fillBean($S, $N)", beanName, beanName);

            classSpecBuilder.addField(fieldSpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void processPreDestroys(MethodSpec.Builder initMethodBuilder) {
        initMethodBuilder.addCode("\n");
        initMethodBuilder.addComment("@PreDestroy hooks");

        for (var pre : preDestroys) {
            var method = pre.getKey();
            var methodName = method.getSimpleName().toString();
            var beanName = pre.getValue();

            var code = CodeBlock.of("$T.addShutdownHook(() -> $N.$N($N))", JabuUtils.class,
                    beanName, methodName, getBeansAsParameters(method));
            initMethodBuilder.addStatement(code);
        }

    }

    public static void processPostConstruct(MethodSpec.Builder initMethodBuilder) {
        initMethodBuilder.addCode("\n");
        initMethodBuilder.addComment("@PostConstruct hooks");

        for (var pre : postConstructs) {
            var method = pre.getKey();
            var methodName = method.getSimpleName().toString();
            var beanName = pre.getValue();
            var code = CodeBlock.of("$N.$N($N)", beanName, methodName, getBeansAsParameters(method));

            initMethodBuilder.addStatement(code);
        }

    }

    public static String getBeansAsParameters(ExecutableElement method) {
        List<String> paramList = new LinkedList<>();
        for (var param : method.getParameters()) {
            paramList.add(getBeanNameForParam(param, method));
        }
        return String.join(", ", paramList);
    }

    public static String getBeanNameForParam(VariableElement param, ExecutableElement method) {
        var q = param.getAnnotation(Qualifier.class);
        String name;
        if (q != null) {
            if (StringUtil.isNotBlank(q.value())) {
                name = q.value().trim();
            } else {
                name = param.getSimpleName().toString();
            }
        } else {
            var opt = getDefaultBeanName(TypeName.get(param.asType()));
            if (opt.isEmpty()) {
                JabuProcessorUtil.fail("Fail to find bean " + param.asType() + " for " + method);
            }
            name = opt.get();
        }
        return name;
    }

    public static void mainLoopForFieldsAndMethods(Element element, String beanName) {
        var injectFields = new HashMap<String, PendingInjectionField>();
        var propFields = new HashMap<String, PendingPropField>();
        var setters = new HashSet<String>();
        for (Element el : element.getEnclosedElements()){
            // Process @Inject fields
            if (el instanceof VariableElement) {
                var field = (VariableElement)el;
                try {
                    // For those @Inject fields
                    var injection = field.getAnnotation(Inject.class);
                    if (injection != null) {
                        var pendingInjection = new PendingInjectionField();
                        pendingInjection.qualifier = injection.qualifier();
                        pendingInjection.fieldName = field.getSimpleName().toString();
                        pendingInjection.fieldType = TypeName.get(field.asType());
                        pendingInjection.objectName = beanName;
                        injectFields.put(pendingInjection.fieldName, pendingInjection);
                    }
                    // For those @Prop fields
                    var prop = field.getAnnotation(Prop.class);
                    if (prop != null) {
                        var pending = new PendingPropField();
                        pending.propPath = prop.value();
                        pending.fieldName = field.getSimpleName().toString();
                        pending.fieldType = TypeName.get(field.asType());
                        pending.objectName = beanName;
                        propFields.put(pending.fieldName, pending);
                    }
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    throw ex;
                }
            }

            if (el instanceof ExecutableElement) {
                preprocessPreDestroy((ExecutableElement)el, beanName);
                preprocessPostConstruct((ExecutableElement)el, beanName);

                var methodName = el.getSimpleName().toString();
                if (methodName.startsWith("set")) {
                    setters.add(methodName);
                }
            }
        }

        // For those @Inject fields
        injectFields.forEach((k, v) -> {
            if (setters.contains(v.setter())) {
                v.hasSetter = true;
            }

            pendingInjectionFields.add(v);
        });

        // For those @Prop fields
        propFields.forEach((k, v) -> {
            if (setters.contains(v.setter())) {
                v.hasSetter = true;
            }

            pendingPropFields.add(v);
        });
    }
    public static void addInjectionStatements(MethodSpec.Builder initMethodBuilder) {
        initMethodBuilder.addCode(CodeBlock.of("\n"));
        initMethodBuilder.addComment("Trying to fill injections, totally %d injections found!".formatted(pendingInjectionFields.size()));

        for (var pending: pendingInjectionFields) {
            String injectBeanName;
            if (pending.qualifier.equals("")) {
                var opt = getDefaultBeanName(pending.fieldType);
                if (opt.isEmpty()) continue;
                injectBeanName = opt.get();
            } else {
                injectBeanName = pending.qualifier;
            }

            if (pending.hasSetter) {
                initMethodBuilder.addStatement(CodeBlock.of("$N.$N(($T)$N)", pending.objectName, pending.setter(), pending.fieldType, injectBeanName));
            } else {
                // With JabuUtils.injectBean(Object obj, String fieldName, Object bean);
                initMethodBuilder.addStatement(CodeBlock.of("$T.injectBean($N, $S, $N)", JabuUtils.class, pending.objectName, pending.fieldName, injectBeanName));
            }

        }
    }

    /**
     * To inject property values to @Prop annotated fields.
     * @param initMethodBuilder
     */
    public static void addInjectPropStatements(MethodSpec.Builder initMethodBuilder) {
        initMethodBuilder.addCode(CodeBlock.of("\n"));
        initMethodBuilder.addComment("Trying to fill @Prop injections, totally %d @Prop found!".formatted(pendingPropFields.size()));

        for (var pending: pendingPropFields) {
            if (pending.propPath.equals("")) {
                JabuProcessorUtil.fail("@Prop value is mandatory, cannot be null or blank");
            }

            if (pending.hasSetter) {
                initMethodBuilder.addStatement(CodeBlock.of("$N.$N(properties.getProperties($S, $T.class))",
                        pending.objectName, pending.setter(), pending.propPath, pending.fieldType));
            } else {
                // With JabuUtils.injectBean(Object obj, String fieldName, Object bean);
                initMethodBuilder.addStatement(CodeBlock.of("$T.injectBean($N, $S, properties.getProperties($S, $T.class))",
                        JabuUtils.class, pending.objectName, pending.fieldName, pending.propPath, pending.fieldType));
            }

        }
    }

    public static void processPendingInjectMethodWithDependencies(TypeSpec.Builder classSpecBuilder,
                                                                  MethodSpec.Builder initMethodBuilder,
                                                                  MethodSpec.Builder getBeanMethodBuilder) {
        if (pendingInjectMethodWithDependencies.isEmpty()) return;

        var sortedList = new LinkedList<PendingInjectMethodWithDependency>();
        var nameSet = new HashSet<String>();
        for (var pending : pendingInjectMethodWithDependencies) {
            nameSet.add(pending.methodBeanName);
        }

        // Sort the pending list by dependencies
        while (!pendingInjectMethodWithDependencies.isEmpty()) {
            for (var pending : pendingInjectMethodWithDependencies) {
                var hasDependency = false;
                for (var param : pending.params) {
                    String paramBeanName;
                    if (StringUtil.isNotBlank(param.qualifier)) {
                        paramBeanName = param.qualifier;
                    } else {
                        // The `beans` contains all bean names already, including method bean with parameters
                        var opt = getDefaultBeanName(param.paramType);
                        if (opt.isEmpty()) JabuProcessorUtil.fail("Not found bean for: " + param.paramType);
                        paramBeanName = opt.get();
                    }
                    // save this name for later processing
                    param.beanNameAfterSorted = paramBeanName;

                    if (nameSet.contains(paramBeanName)){
                        hasDependency = true;
                    }
                }

                if (!hasDependency) {
                    sortedList.add(pending);
                    nameSet.remove(pending.methodBeanName);
                    pendingInjectMethodWithDependencies.remove(pending);
                }
            }
        }

        System.out.println("Sorted pending method beans: ");
        sortedList.forEach(pending -> System.out.println("\tpending methods: " + pending.methodBeanName));

        initMethodBuilder.addCode("\n");
        initMethodBuilder.addComment("Trying to fill method injections with dependencies, totally %d!".formatted(sortedList.size()));
        for (var pending : sortedList) {
            try {
                FieldSpec fieldSpec = FieldSpec.builder(pending.returnType, pending.methodBeanName)
                        .addModifiers(Modifier.PRIVATE)
                        .build();
                getBeanMethodBuilder.addCode(CodeBlock.of("if($S.equals(beanName)) return $N;\n", pending.methodBeanName, pending.methodBeanName));

                initMethodBuilder.addStatement("$N = $T.$N($N)", pending.methodBeanName, pending.belongToClassType,
                        pending.methodName, pending.params.stream().map(param -> param.beanNameAfterSorted).collect(Collectors.joining(",")));
                initMethodBuilder.addStatement("fillBean($S, $N)", pending.methodBeanName, pending.methodBeanName);

                classSpecBuilder.addField(fieldSpec);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Optional<String> getDefaultBeanName(TypeName typeName) {
        if (!beans.containsKey(typeName)) return Optional.empty();
        for (var o : beans.get(typeName)) {
            return Optional.of(o);
        }

        return Optional.empty();
    }

    public static void cacheBeanName(TypeName typeName, String beanName, boolean isSuper) {
        Set<String> m;
        if (beans.containsKey(typeName)) {
            m = beans.get(typeName);
            if (m.contains(beanName)) {
                if (isSuper) return;

                JabuProcessorUtil.fail("Bean " + typeName + " already exists!");
            }
        } else {
            m = new HashSet<>();
            beans.put(typeName, m);
        }
        m.add(beanName);
    }
}
