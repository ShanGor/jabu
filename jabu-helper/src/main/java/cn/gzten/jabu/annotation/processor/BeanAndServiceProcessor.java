package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Inject;
import cn.gzten.jabu.pojo.PendingInjection;
import cn.gzten.jabu.pojo.SimClassInfo;
import cn.gzten.jabu.util.JabuProcessorUtil;
import cn.gzten.jabu.util.JabuUtils;
import com.squareup.javapoet.*;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class BeanAndServiceProcessor {
    private static List<PendingInjection> pendingInjections = new LinkedList<>();
    private static Map<TypeName, Set<String>> beans = new ConcurrentHashMap<>();

    public static <T  extends Annotation> void process(RoundEnvironment roundEnv,
                                                       TypeSpec.Builder classSpecBuilder,
                                                       MethodSpec.Builder initMethodBuilder,
                                                       MethodSpec.Builder getBeanMethodBuilder,
                                                       Class<T> namedAnnotationType) {
        var elements = roundEnv.getElementsAnnotatedWith(namedAnnotationType);
        if (elements.isEmpty()) return;


        for (var e : elements) {
            T beanAnnotation = e.getAnnotation(namedAnnotationType);
            doBeanBasics(namedAnnotationType, beanAnnotation, e, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);
        }
    }

    public static <T  extends Annotation> void doBeanBasics(Class<T> namedAnnotationType,
                                                            T beanAnnotation,
                                                            Element e,
                                                            TypeSpec.Builder classSpecBuilder,
                                                            MethodSpec.Builder initMethodBuilder,
                                                            MethodSpec.Builder getBeanMethodBuilder) {
        String classFullName = e.toString();
        System.out.println("Pre-processing: " + classFullName);

        var classInfo = SimClassInfo.from(classFullName, e);
        AtomicReference<String> nameRef = new AtomicReference<>();
        try {
            Method nameMethod = namedAnnotationType.getDeclaredMethod("name");
            var nameInAnnotation = nameMethod.invoke(beanAnnotation).toString();

            // If the Controller defined customized name, use it, if no, use camel case of the class name as the bean name
            if (StringUtil.isNotBlank(nameInAnnotation)) {
                nameRef.set(nameInAnnotation);
            } else {
                nameRef.set(classInfo.getClassNameCamelCase());
            }
            cacheBeanName(classInfo.getTypeName(), nameRef.get());
        } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException ex) {
            System.out.println("Fatal error at doBeanBasics!");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        addFillBeanCodeBlock(classInfo, nameRef.get(), e, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder);

        fillPendingInjections(e, nameRef.get());
    }

    public static void addFillBeanCodeBlock(SimClassInfo classInfo, String beanName, Element e,
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

            classSpecBuilder.addOriginatingElement(e)
                    .addField(fieldSpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void fillPendingInjections(Element e, String beanName) {
        var fields = new HashMap<String, PendingInjection>();
        var setters = new HashSet<String>();
        for (Element el : e.getEnclosedElements()){
            if (el instanceof VariableElement) {
                var field = (VariableElement)el;
                try {
                    var injection = field.getAnnotation(Inject.class);
                    if (injection == null) continue;

                    var pendingInjection = new PendingInjection();
                    pendingInjection.qualifier = injection.qualifier();
                    pendingInjection.fieldName = field.getSimpleName().toString();
                    pendingInjection.fieldType = TypeName.get(field.asType());
                    pendingInjection.beanName = beanName;
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

            pendingInjections.add(v);
        });
    }

    public static void addInjectionStatements(MethodSpec.Builder initMethodBuilder) {
        initMethodBuilder.addCode(CodeBlock.of("\n"));
        initMethodBuilder.addComment("Trying to fill injections, totally %d injections found!".formatted(pendingInjections.size()));

        for (var pending: pendingInjections) {
            String injectBeanName;
            if (pending.qualifier.equals("")) {
                var opt = getDefaultBeanName(pending.fieldType);
                if (opt.isEmpty()) continue;
                injectBeanName = opt.get();
            } else {
                injectBeanName = pending.qualifier;
            }

            if (pending.hasSetter) {
                initMethodBuilder.addStatement(CodeBlock.of("$N.$N(($T)$N)", pending.beanName, pending.setter(), pending.fieldType, injectBeanName));
            } else {
                // With JabuUtils.injectBean(Object obj, String fieldName, Object bean);
                initMethodBuilder.addStatement(CodeBlock.of("$T.injectBean($N, $S, $N)", JabuUtils.class, pending.beanName, pending.fieldName, injectBeanName));
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
