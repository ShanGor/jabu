package cn.gzten.jabu.pojo;

import cn.gzten.jabu.util.JabuProcessorUtil;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;

public class SimClassInfo {
    public String packageName;
    public String className;
    public String classFullName;

    private Element element;

    public String getClassNameCamelCase() {
        return JabuProcessorUtil.toCamelCase(className);
    }

    public static String convertTypeNameToSimpleCamelCase(TypeName typeName) {
        var tokens = typeName.toString().split("\\.");
        return JabuProcessorUtil.toCamelCase(tokens[tokens.length - 1]);
    }

    public static final SimClassInfo from(final String classFullName, final Element e) {
        SimClassInfo clz = new SimClassInfo();
        clz.classFullName = classFullName;
        clz.element = e;
        if (classFullName.indexOf('.') > 0) {
            clz.packageName = classFullName.substring(0, classFullName.lastIndexOf('.'));
            clz.className = classFullName.substring(classFullName.lastIndexOf('.') + 1);
        } else {
            clz.packageName = "";
            clz.className = classFullName;
        }
        return clz;
    }

    public TypeName getTypeName() {
        return TypeName.get(element.asType());
    }
}
