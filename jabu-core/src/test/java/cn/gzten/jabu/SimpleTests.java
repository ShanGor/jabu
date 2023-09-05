package cn.gzten.jabu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleTests {
    @Test
    public void testListContains() {
        var strList = List.of("str1", "str2", "str3");
        assertTrue(strList.contains("str1"));
    }
}
