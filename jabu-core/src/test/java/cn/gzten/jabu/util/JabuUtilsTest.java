package cn.gzten.jabu.util;

import lombok.Data;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JabuUtilsTest {
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

        var example = pop.toConfig("example", Example.class);

        assertEquals(25, example.id);
        assertEquals("Samuel", example.name);
        assertEquals(2, example.books.size());
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

        assertEquals("""
            {"book":{"name":"Cool book"},"name":"Emerson"}""", JsonUtil.toJson(m));

        m = new HashMap<>();
        m1 = new HashMap<>();


        m.put("name", "Samuel");
        var book = new HashMap<String, Object>();
        book.put("name", "Bye");
        book.put("id", "my id");
        m.put("book", book);

        m1.put("name", "Emerson");
        m1.put("book", Map.of("name", "Cool book"));

        JsonPop.copyMap(m1, m);
        System.out.println(JsonUtil.toJson(m));
        assertEquals("""
                {"book":{"name":"Cool book","id":"my id"},"name":"Emerson"}""", JsonUtil.toJson(m));
    }

    @Data
    public static class Example {
        private String name;
        private int id;
        private List<Book> books;

        @Data
        public static class Book {
            private String name;
        }
    }
}
