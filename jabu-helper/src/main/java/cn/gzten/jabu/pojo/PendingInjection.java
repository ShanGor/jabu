package cn.gzten.jabu.pojo;


import cn.gzten.jabu.util.JabuProcessorUtil;
import com.squareup.javapoet.TypeName;
import org.eclipse.jetty.util.StringUtil;

public class PendingInjection {
    public TypeName fieldType;
    public String beanName;
    public String fieldName;

    public String qualifier;

    public boolean hasSetter = false;

    public String setter() {
        if (StringUtil.isBlank(fieldName)) JabuProcessorUtil.fail("empty field name when processing: " + fieldType.toString());

        return  "set" + JabuProcessorUtil.capitalize(fieldName);
    }
}
