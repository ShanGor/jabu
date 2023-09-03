package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.ConfigurationProperties;
import cn.gzten.jabu.pojo.SimClassInfo;
import com.squareup.javapoet.*;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;

public class ConfigurationPropertiesProcessor {
    public static boolean process(RoundEnvironment roundEnv,
                                  TypeSpec.Builder classSpecBuilder,
                                  MethodSpec.Builder initMethodBuilder,
                                  MethodSpec.Builder getBeanMethodBuilder) {
        var elements = roundEnv.getElementsAnnotatedWith(ConfigurationProperties.class);
        if (elements.isEmpty()) return false;

        for (var element : elements) {
            var ann = element.getAnnotation(ConfigurationProperties.class);
            var typeName = TypeName.get(element.asType());

            String beanName = SimClassInfo.convertTypeNameToSimpleCamelCase(typeName);
            FieldSpec fieldSpec = FieldSpec.builder(TypeName.get(element.asType()), beanName)
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            classSpecBuilder.addField(fieldSpec);
            getBeanMethodBuilder.addCode(CodeBlock.of("if($S.equals(beanName)) return $N;\n", beanName, beanName));

            var prefix = ann.prefix();
            initMethodBuilder.addStatement("$N = this.properties.toConfig($S, $T.class);", beanName, prefix, element.asType());

            BeanProcessor.cacheBeanName(typeName, beanName, false);
        }


        return true;
    }
}
