package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Controller;
import cn.gzten.jabu.pojo.SimClassInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JabuProcessorTest {
    @Test
    public void testClassname() {
        System.out.println(Controller.class.getCanonicalName());
    }

    @Test
    public void testGetCamelCase() {
        var info = new SimClassInfo();
        info.className = "String";
        assertEquals("string", info.getClassNameCamelCase());
    }

    @Test
    public void testReference() {
        String x = "before";
        class X {
            String hey;
        }

        var y = new X();
        y.hey = x;
        x = "after";

        assertEquals("before", y.hey);

    }

}
