package cn.gzten.sim.pojo;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import java.util.Locale;

public class SimClassInfo {
    public String packageName;
    public String className;
    public String classFullName;

    private Element element;

    public String getClassNameCamelCase() {
        if (className == null) return null;
        if (className.length() == 1) {
            return className.toLowerCase(Locale.ROOT);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(className.substring(0, 1).toLowerCase(Locale.ROOT));
        sb.append(className.substring(1));
        return sb.toString();
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
