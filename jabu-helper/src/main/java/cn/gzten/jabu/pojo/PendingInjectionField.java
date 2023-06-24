package cn.gzten.jabu.pojo;


import cn.gzten.jabu.util.JabuProcessorUtil;
import com.squareup.javapoet.TypeName;
import org.eclipse.jetty.util.StringUtil;

public class PendingInjectionField {
    public TypeName fieldType;
    /**
     * This field belongs to an object, the object is a bean kept in the memory.
     */
    public String objectName;
    public String fieldName;

    public String qualifier;

    public boolean hasSetter = false;

    public String setter() {
        if (StringUtil.isBlank(fieldName)) JabuProcessorUtil.fail("empty field name when processing: " + fieldType.toString());

        return  "set" + JabuProcessorUtil.capitalize(fieldName);
    }
}
