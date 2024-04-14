package cn.gzten.jabu.pojo;

import com.squareup.javapoet.TypeName;

import java.util.LinkedList;
import java.util.List;

public class PendingInjectMethodWithDependency {
    public TypeName belongToClassType;
    public TypeName returnType;
    public String methodName;

    public String methodBeanName;

    public List<Param> params = new LinkedList<>();

    public static class Param {
        public TypeName paramType;

        public String propsPath;

        public String qualifier;

        public String paramName;

        /**
         * Initially this should be null, but after sorted, its name should be determined.
         */
        public String beanNameAfterSorted;
    }
}
