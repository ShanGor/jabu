package cn.gzten.pojo;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class Invoker {
    private Object object;
    private Method method;
    private Class clazz;
    private RequestMethod[] httpMethods;
}
