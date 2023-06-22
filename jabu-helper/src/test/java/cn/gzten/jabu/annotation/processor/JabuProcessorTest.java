package cn.gzten.jabu.annotation.processor;

import cn.gzten.jabu.annotation.Controller;
import cn.gzten.jabu.pojo.SimClassInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JabuProcessorTest {
    @Test
    public void testClassname() {
        System.out.println(Controller.class.getCanonicalName());
    }

    @Test
    public void testGetCamelCase() {
        var info = new SimClassInfo();
        info.className = "String";
        Assertions.assertEquals("string", info.getClassNameCamelCase());
    }
}
