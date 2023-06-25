package cn.gzten.example.bean;

import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Qualifier;
import lombok.Data;

import java.io.Serializable;

@HasBean
public class SimpleBeans {
    @Bean
    public static class Hello {}
    @Bean(name = "myWorld")
    @Data
    public static class World implements Serializable {
        private String name = "Samuel";
    }

    @Bean
    public static class Wide { }

    @Bean
    public static String testBeanAtMethod() {
        return "I am a special String";
    }

    public static class BeanA {
        String a;
    }
    public static class BeanB {
        String b;
    }
    public static class BeanC {
        String c;
    }

    @Bean
    public static BeanA testBeanAtMethodWithParam(@Qualifier("myWorld") World world, BeanB b) {
        var a = new BeanA();
        a.a = world.name + ", " + b.b;
        return a;
    }

    @Bean
    public static BeanB beanB(BeanC c) {
        var b = new BeanB();
        b.b = c.c;
        return b;
    }

    @Bean
    public static BeanC beanC(String s) {
        var c = new BeanC();
        c.c = s;
        return c;
    }
}
