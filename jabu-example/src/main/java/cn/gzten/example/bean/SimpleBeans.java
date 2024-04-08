package cn.gzten.example.bean;

import cn.gzten.example.config.DataSourceConfig;
import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Qualifier;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;

import javax.sql.DataSource;
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

    @Bean
    public static DataSource testDatasourceAsBean(DataSourceConfig dataSourceConfig) {
        var ds =  new HikariDataSource();
        ds.setJdbcUrl(dataSourceConfig.getJdbcUrl());
        ds.setUsername(dataSourceConfig.getUser());
        ds.setPassword(dataSourceConfig.getPassword());
        return ds;
    }

    public static class BeanA {
        public String a;
    }
    public static class BeanB {
        public String b;
    }
    public static class BeanC {
        public String c;
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
