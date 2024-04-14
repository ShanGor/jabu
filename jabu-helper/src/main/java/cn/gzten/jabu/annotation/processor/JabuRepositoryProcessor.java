package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.*;
import cn.gzten.jabu.data.CrudRepository;
import cn.gzten.jabu.exception.JabuException;
import cn.gzten.jabu.pojo.SimClassInfo;
import cn.gzten.jabu.util.DatabaseUtil;
import cn.gzten.jabu.util.DateTimeUtils;
import cn.gzten.jabu.util.JabuProcessorUtil;
import cn.gzten.jabu.util.JabuUtils;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class JabuRepositoryProcessor {
    public static List<SimClassInfo> process(RoundEnvironment roundEnv, Filer filer) {
        // Process the @JabuBoot main class, to initialize the Entry Class Path
        var elements = roundEnv.getElementsAnnotatedWith(Repository.class);
        var list = new LinkedList<SimClassInfo>();
        if (elements.isEmpty()) return list;
        elements.iterator().forEachRemaining(element -> {
            var repoAnnotation = element.getAnnotation(Repository.class);
            var tableName = repoAnnotation.table();

            String classFullName = element.toString();
            var classInfo = SimClassInfo.from(classFullName, element);
            list.add(classInfo);
            var classSpecBuilder = TypeSpec.classBuilder(classInfo.className + "Repository")
                    .addSuperinterface(CrudRepository.class)
                    .addModifiers(Modifier.PUBLIC);

            var dataSource = FieldSpec.builder(DataSource.class, "dataSource")
                    .initializer("null").addModifiers(Modifier.PRIVATE, Modifier.STATIC).build();

            var methodFromBuilder = MethodSpec.methodBuilder("from")
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .addParameter(ResultSet.class, "rs")
                    .returns(classInfo.getTypeName());
            composeFromMethod(methodFromBuilder, classInfo, element);

            var methodFindAllBuilder = MethodSpec.methodBuilder("findAll")
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ClassName.get("java.util", "List"), classInfo.getTypeName()));
            var sql = "select * from %s".formatted(tableName);
            methodFindAllBuilder.addStatement("$T sql = $S", String.class, sql);
            var className = ClassName.get(classInfo.packageName, classInfo.className + "Repository");
            methodFindAllBuilder.addStatement("return $T.query(sql, dataSource, $T::from)", DatabaseUtil.class, className);

            var methodForSetDataSource = MethodSpec.methodBuilder("setDataSource")
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .addParameter(DataSource.class, "ds")
                    .addStatement("dataSource = ds");

            // build the class spec.
            var spec = classSpecBuilder.addField(dataSource)
                    .addMethod(methodFromBuilder.build())
                    .addMethod(methodFindAllBuilder.build())
                    .addMethod(methodForSetDataSource.build())
                    .build();

            try {
                JavaFile.builder(classInfo.packageName, spec).build().writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        return list;
    }

    public static void composeFromMethod(MethodSpec.Builder methodFromBuilder, SimClassInfo classInfo, Element element) {
        methodFromBuilder.addStatement(CodeBlock.of("$T obj = new $T()", classInfo.getTypeName(), classInfo.getTypeName()));

        AtomicBoolean hasMember = new AtomicBoolean(false);
        element.getEnclosedElements().forEach(member -> {
            if (member instanceof VariableElement) {
                var variableElement = (VariableElement) member;
                var fieldName = variableElement.getSimpleName().toString();
                var fieldType = variableElement.asType();
                var setter = JabuUtils.composeSetter(fieldName);
                var key = JabuUtils.camelCaseToUnderscore(fieldName);
                if (!hasMember.get()) {
                    hasMember.set(true);
                    methodFromBuilder.addCode("try {\n");
                }
                if (JabuProcessorUtil.typeMirrorIsType(fieldType, String.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getString($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, Integer.class)|| JabuProcessorUtil.typeMirrorIsType(fieldType, int.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getInt($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, Long.class)|| JabuProcessorUtil.typeMirrorIsType(fieldType, long.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getLong($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, Float.class)|| JabuProcessorUtil.typeMirrorIsType(fieldType, float.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getFloat($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, Double.class)|| JabuProcessorUtil.typeMirrorIsType(fieldType, double.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getDouble($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, BigDecimal.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getBigDecimal($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, Boolean.class)|| JabuProcessorUtil.typeMirrorIsType(fieldType, boolean.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getBoolean($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, java.sql.Date.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getDate($S))", setter, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, java.util.Date.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L($T.fromDate(rs.getDate($S)))", setter, DateTimeUtils.class, key));
                } else if (JabuProcessorUtil.typeMirrorIsType(fieldType, java.sql.Timestamp.class)) {
                    methodFromBuilder.addStatement(CodeBlock.of("obj.$L(rs.getTimestamp($S))", setter, key));
                } else {
                    throw new RuntimeException("Unsupported type:　％ｓ".formatted(fieldType));
                }
            }
        });
        if (hasMember.get()) {
            methodFromBuilder.addCode("} catch($T e) {\n", SQLException.class);
            methodFromBuilder.addStatement("throw new $T(e)", JabuException.class);
            methodFromBuilder.addCode("}\n");
        }
        methodFromBuilder.addStatement(CodeBlock.of("return obj"));
    }
}
