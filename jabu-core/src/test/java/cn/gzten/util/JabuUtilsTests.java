package cn.gzten.util;

import cn.gzten.jabu.util.JabuUtils;
import cn.gzten.jabu.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JabuUtilsTests {
    @Test
    public void testStringInArray() {
        String str = null;
        String[] array = null;

        assertFalse(JabuUtils.stringInArray(str, array));
        str = "";
        assertFalse(JabuUtils.stringInArray(str, array));
        str = "hey";
        assertFalse(JabuUtils.stringInArray(str, array));
        str = "hey";
        array = new String[]{};
        assertFalse(JabuUtils.stringInArray(str, array));
        array = new String[]{"you"};
        assertFalse(JabuUtils.stringInArray(str, array));
        array = new String[]{"you", "hey"};
        assertTrue(JabuUtils.stringInArray(str, array));
    }

    @Test
    public void testStringInArrayCaseInsensitive() {

        String str = null;
        String[] array = null;

        assertFalse(JabuUtils.stringInArrayCaseInsensitive(str, array));
        str = "";
        assertFalse(JabuUtils.stringInArrayCaseInsensitive(str, array));
        str = "hey";
        assertFalse(JabuUtils.stringInArrayCaseInsensitive(str, array));
        str = "hey";
        array = new String[]{};
        assertFalse(JabuUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you"};
        assertFalse(JabuUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you", "hey"};
        assertTrue(JabuUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you", "Hey"};
        assertTrue(JabuUtils.stringInArrayCaseInsensitive(str, array));
    }

    @Test
    public void testJson() {
        System.out.println(JsonUtil.toJson(1));
    }

    @Test
    public void testCapitalize() {
        assertEquals(" hey", JabuUtils.capitalize(" hey"));
        assertEquals(" ", JabuUtils.capitalize(" "));
        assertNull(JabuUtils.capitalize(null));
        assertEquals("Hey", JabuUtils.capitalize("hey"));
        assertEquals("Hey", JabuUtils.capitalize("Hey"));
    }

    @Test
    public void testLowercaseInitial() {
        assertEquals(" hey", JabuUtils.lowercaseInitial(" hey"));
        assertEquals(" ", JabuUtils.lowercaseInitial(" "));
        assertNull(JabuUtils.lowercaseInitial(null));
        assertEquals("hey", JabuUtils.lowercaseInitial("hey"));
        assertEquals("hey", JabuUtils.lowercaseInitial("Hey"));
    }

    @Test
    public void testComposeSetter() {
        assertEquals("setHey", JabuUtils.composeSetter("Hey"));
        assertEquals("setHey", JabuUtils.composeSetter("hey"));
    }

    @Test
    public void testWireBean() {
        class A {
            private String hey;
        }
        var a = new A();
        JabuUtils.wireBean(a, "hey", "you");
        assertEquals("you", a.hey);
    }
}
