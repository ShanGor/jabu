package cn.gzten.example.bean;

import cn.gzten.jabu.annotation.Bean;
import cn.gzten.jabu.annotation.HasBean;
import cn.gzten.jabu.annotation.Qualifier;
import lombok.Data;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.*;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

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

    /**
     * Do not use this Request, it is for the purpose of testing compilation
     * @return
     */
    @Bean
    public static Request testRequestAsBean() {
        return new Request() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public Components getComponents() {
                return null;
            }

            @Override
            public ConnectionMetaData getConnectionMetaData() {
                return null;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public HttpURI getHttpURI() {
                return null;
            }

            @Override
            public Context getContext() {
                return null;
            }

            @Override
            public HttpFields getHeaders() {
                return null;
            }

            @Override
            public void demand(Runnable demandCallback) {

            }

            @Override
            public HttpFields getTrailers() {
                return null;
            }

            @Override
            public long getTimeStamp() {
                return 0;
            }

            @Override
            public long getNanoTime() {
                return 0;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public Content.Chunk read() {
                return null;
            }

            @Override
            public boolean consumeAvailable() {
                return false;
            }

            @Override
            public boolean addErrorListener(Predicate<Throwable> onError) {
                return false;
            }

            @Override
            public TunnelSupport getTunnelSupport() {
                return null;
            }

            @Override
            public void addHttpStreamWrapper(Function<HttpStream, HttpStream> wrapper) {

            }

            @Override
            public Session getSession(boolean create) {
                return null;
            }

            @Override
            public void fail(Throwable throwable) {

            }

            @Override
            public Object removeAttribute(String name) {
                return null;
            }

            @Override
            public Object setAttribute(String name, Object attribute) {
                return null;
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public Set<String> getAttributeNameSet() {
                return null;
            }
        };
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
