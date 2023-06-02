package cn.gzten.sim.annotation.processor;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;
import cn.gzten.pojo.RequestMethod;
import cn.gzten.sim.pojo.SimClassInfo;
import cn.gzten.sim.util.SimProcessorUtil;
import cn.gzten.util.SimUtils;
import com.squareup.javapoet.*;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerProcessor {
    public static boolean process(RoundEnvironment roundEnv,
                                  TypeSpec.Builder classSpecBuilder,
                                  MethodSpec.Builder initMethodBuilder,
                                  MethodSpec.Builder getBeanMethodBuilder,
                                  MethodSpec.Builder tryProcessRouteMethodBuilder) {
        var elements = roundEnv.getElementsAnnotatedWith(Controller.class);
        if (elements.isEmpty()) return false;

        tryProcessRouteMethodBuilder.addCode("var requestPath = ctx.getPath();\n");
        tryProcessRouteMethodBuilder.addCode("var requestMethod = ctx.getRequest().getMethod();\n\n");

        AtomicReference<String> nameRef = new AtomicReference<>();

        Set<String> existingRoutes = new HashSet<>();
        for (var e : elements) {
            Controller controllerAnnotation = e.getAnnotation(Controller.class);

            String classFullName = e.toString();
            System.out.println("Pre-processing: " + classFullName);

            var classInfo = SimClassInfo.from(classFullName, e);
            if (StringUtil.isNotBlank(controllerAnnotation.name())) {
                nameRef.set(controllerAnnotation.name());
            } else {
                nameRef.set(classInfo.getClassNameCamelCase());
            }

            // $L(for Literals), $S(for Strings), $T(for Types), $N(for Names), as placeholders
            try {
                FieldSpec fieldSpec = FieldSpec.builder(classInfo.getTypeName(), nameRef.get())
                        .addModifiers(Modifier.PRIVATE)
                        .build();
                getBeanMethodBuilder.addCode(CodeBlock.of("if($N.equals(beanName)) return $N;\n", nameRef.get(), nameRef.get()));
                initMethodBuilder.addCode("$N = new $N();\n", nameRef.get(), classInfo.className);

                initMethodBuilder.addCode("fillBean($N.class, $S, $N);", classInfo.className,
                        nameRef.get(), nameRef.get());

                classSpecBuilder.addOriginatingElement(e)
                        .addField(fieldSpec);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (Element el : e.getEnclosedElements()){
                if (el instanceof ExecutableElement) {
                    var method = (ExecutableElement)el;
                    try {
                        RequestMapping reqMapAnnotation = method.getAnnotation(RequestMapping.class);
                        if (reqMapAnnotation != null) {
                            System.out.println("Found request mapping method: " + method.getSimpleName().toString());

                            var methodParamParseResult = SimProcessorUtil.prepareMethodParameters(method.getParameters());

                            var uniqueRouteEntry = reqMapAnnotation.path() + "." + RequestMethod.serializeArray(reqMapAnnotation.method());
                            if (existingRoutes.contains(uniqueRouteEntry)) {
                                var errMsg = "Duplicate route entry found: " + uniqueRouteEntry;
                                System.err.println(errMsg);
                                throw new RuntimeException(errMsg);
                            } else {
                                existingRoutes.add(uniqueRouteEntry);
                            }

                            // If http method list is empty, then supports all methods
                            String templatePrefix;
                            String templateSuffix;
                            String blankPrefix;
                            if (SimUtils.isEmpty(reqMapAnnotation.method()) ) {
                                templatePrefix = """
                                        // for $N.$N: $N
                                        {
                                          if ($T.matchPath(requestPath, $S, $L)) {
                                          """;

                                templateSuffix = """
                                            return;
                                          }
                                        }
                                        """;
                                blankPrefix = "    ";

                                tryProcessRouteMethodBuilder.addCode(templatePrefix, classFullName, method.getSimpleName().toString(), reqMapAnnotation.path(),
                                        SimUtils.class, reqMapAnnotation.path(), reqMapAnnotation.regex());
                            } else {
                                templatePrefix = """
                                        // for $N.$N: $N
                                        {
                                          var methods = new $T[]{$N};
                                          if ($T.httpMethodMatches(requestMethod, methods)) {
                                            if (SimUtils.matchPath(requestPath, $S, $L)) {
                                            """;

                                templateSuffix = """
                                              return;
                                            }
                                          }
                                        }
                                        """;
                                blankPrefix = "      ";

                                tryProcessRouteMethodBuilder.addCode(templatePrefix, classFullName, method.getSimpleName().toString(), reqMapAnnotation.path(),
                                        RequestMethod.class, RequestMethod.serializeArray(reqMapAnnotation.method()),
                                        SimUtils.class,
                                        reqMapAnnotation.path(), reqMapAnnotation.regex());
                            }

                            methodParamParseResult.codes.forEach(codeBlock -> tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                    .addCode(codeBlock));
                            var returnType = ClassName.get(method.getReturnType());
                            System.out.println("Found return type: " + returnType.toString());
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
                                        .addStatement("var _res = $N.$N($N);", nameRef.get(), method.getSimpleName().toString(), String.join(", ", methodParamParseResult.paramNames));

                                // Different return type, different handling way
                                if (returnType.isPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(String.valueOf(_res))");
                                } else if(returnType.isBoxedPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(_res.toString())");
                                } else if (returnType.equals(TypeName.get(String.class))) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(_res)");
                                } else {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addStatement("ctx.returnWith(JsonUtil.toJson(_res))");
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
        tryProcessRouteMethodBuilder.addStatement(CodeBlock.of("ctx.returnWith(404, $S + requestPath)", "Not found proper route for "));

        return true;
    }
}
