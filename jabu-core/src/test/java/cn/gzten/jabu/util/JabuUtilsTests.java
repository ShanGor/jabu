package cn.gzten.jabu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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
    public void testInjectBean() {
        class A {
            private String hey;
        }
        var a = new A();
        JabuUtils.injectBean(a, "hey", "you");
        assertEquals("you", a.hey);
    }

    @Test
    public void testGetPathVariables() {
        var pattern = "/books/{id}";
        var endpoint = "/books/12";

        var res = JabuUtils.getPathVariables(pattern, endpoint);
        assertEquals("12", res.get().get("id"));

        pattern = "/books/{id}/details";
        endpoint = "/books/12/details";
        res = JabuUtils.getPathVariables(pattern, endpoint);
        assertEquals("12", res.get().get("id"));

        pattern = "/books/{year}/{month}";
        endpoint = "/books/2023/12";
        res = JabuUtils.getPathVariables(pattern, endpoint);
        assertEquals("2023", res.get().get("year"));
        assertEquals("12", res.get().get("month"));

        assertTrue(Pattern.compile("/m/a/.*/h").matcher("/m/a/c/ad/h").matches());
        assertFalse(Pattern.compile("/m/a/.*/h").matcher("/m/a/h").matches());
        assertTrue(Pattern.compile("/m/a/.*").matcher("/m/a/").matches());


        pattern = "/books/{year}/{month}/**";
        endpoint = "/books/2023/12/hey/you";
        res = JabuUtils.getPathVariables(pattern, endpoint);
        assertEquals("2023", res.get().get("year"));
        assertEquals("12", res.get().get("month"));

        pattern = "/books/*/{year}/{month}";
        endpoint = "/books/hey/2023/12";
        res = JabuUtils.getPathVariables(pattern, endpoint);
        assertEquals("2023", res.get().get("year"));
        assertEquals("12", res.get().get("month"));
    }

    @Test
    public void testProperties() {
        var ins = ClassLoader.getSystemClassLoader().getResourceAsStream("application.json");
        JsonPop pop = new JsonPop();
        pop.load(ins);

        assertEquals("Samuel", pop.getProperties("example.name", String.class));

        Integer id = pop.getProperties("example.id", Integer.class);
        assertEquals(25, id);
        System.out.println(pop.getProperties( "example.books[0].name", String.class));

        pop.put("server.port", 8080);
        assertEquals(8080, pop.getProperties("server.port", Integer.class));
    }

    @Test
    public void testMap() {
        var m = new HashMap<String, Object >();
        var m1 = new HashMap<String, Object>();
        m1.put("name", "Emerson");

        m.put("name", "Samuel");
        m.putAll(m1);

        assertEquals("Emerson", m.get("name"));

        m.put("book", Map.of("name", "Bye", "id", "my id"));
        m1.put("book", Map.of("name", "Cool book"));

        m.putAll(m1);

        System.out.println(JsonUtil.toJson(m));


    }
}
