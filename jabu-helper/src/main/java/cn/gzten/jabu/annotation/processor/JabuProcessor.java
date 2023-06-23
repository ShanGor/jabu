package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.*;
import cn.gzten.jabu.core.JabuContext;
import cn.gzten.jabu.JabuEntry;
import cn.gzten.jabu.pojo.SimClassInfo;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_19)
@AutoService(Processor.class)
public class JabuProcessor extends AbstractProcessor {
    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        System.out.println("ControllerProcessor done initialization!");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(JabuBoot.class.getCanonicalName(), Controller.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var classSpecBuilder = TypeSpec.classBuilder("JabuEntryImpl")
                .superclass(JabuEntry.class)
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
                .addParameter(JabuContext.class, "ctx");

        var elements = roundEnv.getElementsAnnotatedWith(JabuBoot.class);
        if (elements.isEmpty()) return false;
        if (elements.size() != 1) {
            System.err.println("Found 0 / more-than-1 @JabuBoot classes, compile failed!");
            throw new RuntimeException("Found 0 / more-than-1 @JabuBoot classes, compile failed!");
        }
        Element bootMain = (Element) elements.toArray()[0];
        String classFullName = bootMain.toString();
        var bootClassInfo = SimClassInfo.from(classFullName, bootMain);

        // Process @Controller cases
        var processResult = ControllerProcessor.process(roundEnv, classSpecBuilder, initMethodBuilder, getBeanMethodBuilder, tryProcessRouteMethodBuilder );
        if (processResult == false) return false;

        getBeanMethodBuilder.addCode(CodeBlock.of("\nreturn null;"));

        var spec = classSpecBuilder
                .addMethod(initMethodBuilder.build())
                .addMethod(getBeanMethodBuilder.build())
                .addMethod(tryProcessRouteMethodBuilder.build())
                .build();

        try {
            JavaFile.builder(bootClassInfo.packageName, spec).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return true;
    }

}
