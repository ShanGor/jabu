package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Controller;
import cn.gzten.jabu.annotation.Route;
import cn.gzten.jabu.pojo.RequestMethod;
import cn.gzten.jabu.pojo.SimClassInfo;
import cn.gzten.jabu.util.JabuProcessorUtil;
import cn.gzten.jabu.util.JabuUtils;
import com.squareup.javapoet.*;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerProcessor {
    public static boolean process(RoundEnvironment roundEnv, MethodSpec.Builder tryProcessRouteMethodBuilder) {
        var elements = roundEnv.getElementsAnnotatedWith(Controller.class);
        if (elements.isEmpty()) return false;

        tryProcessRouteMethodBuilder.addCode("var requestPath = ctx.getPath();\n");
        tryProcessRouteMethodBuilder.addCode("var requestMethod = ctx.getRequest().getMethod();\n\n");

        AtomicReference<String> nameRef = new AtomicReference<>();

        Set<String> existingRoutes = new HashSet<>();
        for (var e : elements) {
            Controller controllerAnnotation = e.getAnnotation(Controller.class);
            var basePath = controllerAnnotation.basePath();

            String classFullName = e.toString();
            System.out.println("Pre-processing controller: " + classFullName);

            var classInfo = SimClassInfo.from(classFullName, e);

            // If the Controller defined customized name, use it, if no, use camel case of the class name as the bean name
            if (StringUtil.isNotBlank(controllerAnnotation.name())) {
                nameRef.set(controllerAnnotation.name());
            } else {
                nameRef.set(classInfo.getClassNameCamelCase());
            }

            for (Element el : e.getEnclosedElements()){
                if (el instanceof ExecutableElement) {
                    var method = (ExecutableElement)el;
                    try {
                        Route reqMapAnnotation = method.getAnnotation(Route.class);
                        if (reqMapAnnotation != null) {
                            System.out.println("Found request mapping method: " + method.getSimpleName().toString());

                            var methodParamParseResult = JabuProcessorUtil.prepareMethodParameters(method.getParameters());

                            var requestPathPattern = basePath + reqMapAnnotation.path();

                            var uniqueRouteEntry = requestPathPattern + "." + RequestMethod.serializeArray(reqMapAnnotation.method());
                            if (existingRoutes.contains(uniqueRouteEntry)) {
                                JabuProcessorUtil.fail("Duplicate route entry found: " + uniqueRouteEntry);
                            } else {
                                existingRoutes.add(uniqueRouteEntry);
                            }

                            // If http method list is empty, then supports all methods
                            String templatePrefix;
                            String templateSuffix;
                            String blankPrefix;
                            if (JabuUtils.isEmpty(reqMapAnnotation.method()) ) {
                                templatePrefix = """
                                        // for $N.$N: $N
                                        {
                                          if ($T.matchPath(ctx, requestPath, $S, $L)) {
                                          """;

                                templateSuffix = """
                                            return true;
                                          }
                                        }
                                        """;
                                blankPrefix = "    ";

                                tryProcessRouteMethodBuilder.addCode(templatePrefix, classFullName, method.getSimpleName().toString(), requestPathPattern,
                                        JabuUtils.class, requestPathPattern, reqMapAnnotation.regex());
                            } else {
                                templatePrefix = """
                                        // for $N.$N: $N
                                        {
                                          var methods = new $T[]{$N};
                                          if ($T.httpMethodMatches(requestMethod, methods)) {
                                            if (JabuUtils.matchPath(ctx, requestPath, $S, $L)) {
                                            """;

                                templateSuffix = """
                                              return true;
                                            }
                                          }
                                        }
                                        """;
                                blankPrefix = "      ";

                                tryProcessRouteMethodBuilder.addCode(templatePrefix, classFullName, method.getSimpleName().toString(), requestPathPattern,
                                        RequestMethod.class, RequestMethod.serializeArray(reqMapAnnotation.method()),
                                        JabuUtils.class,
                                        requestPathPattern, reqMapAnnotation.regex());
                            }

                            methodParamParseResult.codes.forEach(codeBlock -> tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                    .addCode(codeBlock));
                            var returnType = ClassName.get(method.getReturnType());
                            if (returnType.equals(TypeName.VOID)) {
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addStatement("ctx.withStatus(200)");
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addStatement("$N.$N($N)", nameRef.get(), method.getSimpleName().toString(), String.join(", ", methodParamParseResult.paramNames));
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addStatement("ctx.returnVoid()");
                            } else {
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addStatement("ctx.withStatus(200)");
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addStatement("var _res = $N.$N($N)", nameRef.get(), method.getSimpleName().toString(), String.join(", ", methodParamParseResult.paramNames));

                                // Different return type, different handling way
                                if (returnType.isPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(String.valueOf(_res))");
                                } else if(returnType.isBoxedPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(_res.toString())");
                                } else {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(_res)");
                                }
                            }

                            tryProcessRouteMethodBuilder.addCode(templateSuffix);
                        }
                    } catch (RuntimeException re) {
                        re.printStackTrace();
                        throw re;
                    }
                }
            }
        }
        // Not found route
        tryProcessRouteMethodBuilder.addComment("Not found route");
        tryProcessRouteMethodBuilder.addStatement(CodeBlock.of("return false"));

        return true;
    }
}
