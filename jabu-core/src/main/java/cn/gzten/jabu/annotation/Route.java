package cn.gzten.jabu.annotation;


import cn.gzten.jabu.pojo.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Route {
    String path();
    RequestMethod[] method() default {};
    boolean regex() default false;
}
