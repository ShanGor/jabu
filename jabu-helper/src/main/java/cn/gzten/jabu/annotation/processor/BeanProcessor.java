package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Inject;
import cn.gzten.jabu.pojo.PendingInjectionField;
import cn.gzten.jabu.pojo.SimClassInfo;
import cn.gzten.jabu.util.JabuProcessorUtil;
import cn.gzten.jabu.util.JabuUtils;
import com.squareup.javapoet.*;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class BeanProcessor {
    private static List<PendingInjectionField> pendingInjectionFields = new LinkedList<>();
    private static Map<TypeName, Set<String>> beans = new ConcurrentHashMap<>();

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
        String classFullName = element.toString();
        System.out.println("Pre-processing: " + classFullName);
        element.getInterfaces().forEach(m -> {
            System.out.println("\t interfaces " + m.toString());
        });
        System.out.println("\t Super class " + element.getSuperclass());

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
                cacheBeanName(classInfo.getTypeName(), nameRef.get());
            } catch (InvocationTargetException|IllegalAccessException ex) {
                ex.printStackTrace();
                JabuProcessorUtil.fail("Fatal error at doBeanBasics!");
            }

            addFillBeanCodeBlock(classInfo, nameRef.get(), classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);

            fillPendingInjectionFields(element, nameRef.get());

            return;
        }

        if (HasBean.class.equals(annotationType)) {
            for (Element el : element.getEnclosedElements()){
                if (!(el instanceof ExecutableElement)) continue;

                var beanMethodAnnotation = el.getAnnotation(Bean.class);
                if (beanMethodAnnotation == null) continue;

                preprocessMethodBean(element, (ExecutableElement)el, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
            }
        }
    }

    public static void preprocessMethodBean(TypeElement clazz, ExecutableElement element,
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
            nameRef.set(element.getSimpleName().toString());
        }

        // Very simple no dependency
        if (params.isEmpty()) {
            addFillBeanCodeBlockForMethodBeanWithEmptyParams(clazz.asType(), returnType, nameRef.get(),
                    element.getSimpleName().toString(),
                    classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
        } else {
            //TODO for those parameters, you need to prioritize them for filling dependencies
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

    public static Optional<String> getDefaultBeanName(TypeName typeName) {
        if (!beans.containsKey(typeName)) return Optional.empty();
        for (var o : beans.get(typeName)) {
            return Optional.of(o);
        }

        return Optional.empty();
    }

    public static void cacheBeanName(TypeName typeName, String beanName) {
        Set<String> m;
        if (beans.containsKey(typeName)) {
            m = beans.get(typeName);
            if (m.contains(beanName)) {
                JabuProcessorUtil.fail("Bean " + typeName + " already exists!");
            }
        } else {
            m = new HashSet<>();
            beans.put(typeName, m);
        }
        m.add(beanName);
    }
}
