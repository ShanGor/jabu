package cn.gzten.jabu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To annotate a POJO as DynamoBean, will also register it as RegisterReflection. to add to reflect-config.json
 * automatically.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Bean {
    String name() default "";
}
