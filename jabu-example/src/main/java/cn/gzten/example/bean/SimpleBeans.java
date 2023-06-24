package cn.gzten.example.bean;

import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
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
        return "";
    }

}
