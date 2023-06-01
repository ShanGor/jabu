package cn.gzten.sim.annotation.processor;

import cn.gzten.annotation.*;
import cn.gzten.pojo.RequestMethod;
import cn.gzten.sim.JSimEntry;
import cn.gzten.sim.pojo.SimClassInfo;
import cn.gzten.sim.util.SimProcessorUtil;
import cn.gzten.util.SimUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.StringUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SupportedSourceVersion(SourceVersion.RELEASE_19)
@AutoService(Processor.class)
public class JSimProcessor extends AbstractProcessor {
    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        System.out.println("ControllerProcessor done initialization!");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Controller.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elements = roundEnv.getElementsAnnotatedWith(Controller.class);

        if (elements.isEmpty()) return false;

        var classSpecBuilder = TypeSpec.classBuilder("JSimEntryImpl")
                .superclass(JSimEntry.class)
                .addModifiers(Modifier.PUBLIC);
        var getBeanMethodBuilder = MethodSpec.methodBuilder("getBean")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "beanName")
                .returns(Object.class);
        var initMethodBuilder = MethodSpec.methodBuilder("init")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC);

        var tryProcessRouteMethodBuilder = MethodSpec.methodBuilder("tryProcessRoute")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Request.class, "request")
                .addParameter(Response.class, "response")
                .addParameter(Callback.class, "callback");
        tryProcessRouteMethodBuilder.addCode("var requestPath = request.getHttpURI().getPath();\n");
        tryProcessRouteMethodBuilder.addCode("var requestMethod = request.getMethod();\n\n");

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

                            methodParamParseResult.codes.forEach(codeBlock -> tryProcessRouteMethodBuilder.addCode("      ")
                                    .addCode(codeBlock));
                            var returnType = ClassName.get(method.getReturnType());
                            System.out.println("Found return type: " + returnType.toString());
                            if (returnType.equals(TypeName.VOID)) {
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addCode("response.setStatus(200);\n");
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addCode("$N.$N($N);\n", nameRef.get(), method.getSimpleName().toString(), String.join(", ", methodParamParseResult.paramNames));
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addCode("callback.succeeded();\n");
                            } else {
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addCode("response.setStatus(200);\n");
                                tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                        .addCode("var _res = $N.$N($N);\n", nameRef.get(), method.getSimpleName().toString(), String.join(", ", methodParamParseResult.paramNames));

                                // Different return type, different handling way
                                if (returnType.isPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addCode("response.write(true, $T.wrap(String.valueOf(_res).getBytes()), callback);\n", ByteBuffer.class);
                                } else if(returnType.isBoxedPrimitive()) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addCode("response.write(true, $T.wrap(_res.toString().getBytes()), callback);\n", ByteBuffer.class);
                                } else if (returnType.equals(TypeName.get(String.class))) {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addCode("response.write(true, $T.wrap(_res.getBytes()), callback);\n", ByteBuffer.class);
                                } else {
                                    tryProcessRouteMethodBuilder.addCode(blankPrefix)
                                            .addCode("response.write(true, $T.wrap(JsonUtil.toJson(_res).getBytes()), callback);\n", ByteBuffer.class);
                                }
                            }

                            tryProcessRouteMethodBuilder.addCode(templateSuffix);
                        }
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                        continue;
                    }
                }
            }
        }

        getBeanMethodBuilder.addCode(CodeBlock.of("\nreturn null;"));

        var spec = classSpecBuilder
                .addMethod(initMethodBuilder.build())
                .addMethod(getBeanMethodBuilder.build())
                .addMethod(tryProcessRouteMethodBuilder.build())
                .build();

        try {
            JavaFile.builder("cn.gzten.sim", spec).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return true;
    }

}
