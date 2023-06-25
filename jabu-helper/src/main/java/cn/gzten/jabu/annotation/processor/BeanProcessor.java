package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Inject;
import cn.gzten.jabu.annotation.Qualifier;
import cn.gzten.jabu.pojo.PendingInjectMethodWithDependency;
import cn.gzten.jabu.pojo.PendingInjectionField;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BeanProcessor {
    private static final List<PendingInjectionField> pendingInjectionFields = new LinkedList<>();
    private static final Map<TypeName, Set<String>> beans = new ConcurrentHashMap<>();

    private static final List<PendingInjectMethodWithDependency> pendingInjectMethodWithDependencies = new LinkedList<>();

    public static <T  extends Annotation> void process(RoundEnvironment roundEnv,
                                                       TypeSpec.Builder classSpecBuilder,
                                                       MethodSpec.Builder initMethodBuilder,
                                                       MethodSpec.Builder getBeanMethodBuilder,
                                                       Class<T> annotationType) {
        var elements = roundEnv.getElementsAnnotatedWith(annotationType);
        if (elements.isEmpty()) return;

        for (var element : elements) {
            T beanAnnotation = element.getAnnotation(annotationType);
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
        if (HasBean.class.equals(annotationType)) {
            for (Element el : element.getEnclosedElements()){
                if (!(el instanceof ExecutableElement)) continue;

                var beanMethodAnnotation = el.getAnnotation(Bean.class);
                if (beanMethodAnnotation == null) continue;

                preprocessMethodBean(element, (ExecutableElement)el, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
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

            fillPendingInjectionFields(element, nameRef.get());
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

            //TODO: add implemented interfaces (excluding Serializable) and superclass (only one layer, exclude Object)
            initMethodBuilder.addStatement("fillBean($S, $N)", beanName, beanName);

            classSpecBuilder.addField(fieldSpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void fillPendingInjectionFields(Element element, String beanName) {
        var fields = new HashMap<String, PendingInjectionField>();
        var setters = new HashSet<String>();
        for (Element el : element.getEnclosedElements()){
            if (el instanceof VariableElement) {
                var field = (VariableElement)el;
                try {
                    var injection = field.getAnnotation(Inject.class);
                    if (injection == null) continue;

                    var pendingInjection = new PendingInjectionField();
                    pendingInjection.qualifier = injection.qualifier();
                    pendingInjection.fieldName = field.getSimpleName().toString();
                    pendingInjection.fieldType = TypeName.get(field.asType());
                    pendingInjection.objectName = beanName;
                    fields.put(pendingInjection.fieldName, pendingInjection);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    throw ex;
                }
            }

            if (el instanceof ExecutableElement) {
                var methodName = el.getSimpleName().toString();
                if (methodName.startsWith("set")) {
                    setters.add(methodName);
                }
            }
        }

        fields.forEach((k, v) -> {
            if (setters.contains(v.setter())) {
                v.hasSetter = true;
            }

            pendingInjectionFields.add(v);
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
