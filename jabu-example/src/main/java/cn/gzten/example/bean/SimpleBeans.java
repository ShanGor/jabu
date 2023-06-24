package cn.gzten.example.bean;

import cn.gzten.jabu.annotation.Bean;
import lombok.Data;

public class SimpleBeans {
    @Bean
    public static class Hello {}
    @Bean(name = "myWorld")
    @Data
    public static class World {
        private String name = "Samuel";
    }

    @Bean
    public static class Wide { }

}
