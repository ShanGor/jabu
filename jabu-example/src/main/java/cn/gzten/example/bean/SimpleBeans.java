package cn.gzten.example.bean;

import cn.gzten.example.config.DataSourceConfig;
import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Prop;
import cn.gzten.jabu.annotation.Qualifier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Serializable;

@Slf4j
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
    public static DataSource dataSource(DataSourceConfig dataSourceConfig, @Prop("dataSource.jdbcUrl") String jdbcUrl) {
        log.info("jdbcUrl is {}", jdbcUrl);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceConfig.getJdbcUrl());
        config.setUsername(dataSourceConfig.getUser());
        config.setPassword(dataSourceConfig.getPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
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
