package cn.gzten.sim.annotation.processor;

import cn.gzten.annotation.Controller;
import cn.gzten.sim.pojo.SimClassInfo;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JSimProcessorTest {
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
