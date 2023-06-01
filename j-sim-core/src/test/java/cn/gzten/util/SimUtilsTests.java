package cn.gzten.util;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimUtilsTests {
    @Test
    public void testStringInArray() {
        String str = null;
        String[] array = null;

        assertFalse(SimUtils.stringInArray(str, array));
        str = "";
        assertFalse(SimUtils.stringInArray(str, array));
        str = "hey";
        assertFalse(SimUtils.stringInArray(str, array));
        str = "hey";
        array = new String[]{};
        assertFalse(SimUtils.stringInArray(str, array));
        array = new String[]{"you"};
        assertFalse(SimUtils.stringInArray(str, array));
        array = new String[]{"you", "hey"};
        assertTrue(SimUtils.stringInArray(str, array));
    }

    @Test
    public void testStringInArrayCaseInsensitive() {

        String str = null;
        String[] array = null;

        assertFalse(SimUtils.stringInArrayCaseInsensitive(str, array));
        str = "";
        assertFalse(SimUtils.stringInArrayCaseInsensitive(str, array));
        str = "hey";
        assertFalse(SimUtils.stringInArrayCaseInsensitive(str, array));
        str = "hey";
        array = new String[]{};
        assertFalse(SimUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you"};
        assertFalse(SimUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you", "hey"};
        assertTrue(SimUtils.stringInArrayCaseInsensitive(str, array));
        array = new String[]{"you", "Hey"};
        assertTrue(SimUtils.stringInArrayCaseInsensitive(str, array));
    }

    @Test
    public void testJson() {
        System.out.println(JsonUtil.toJson(1));
    }
}
