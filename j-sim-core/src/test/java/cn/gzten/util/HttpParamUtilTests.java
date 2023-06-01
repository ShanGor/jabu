package cn.gzten.util;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class HttpParamUtilTests {
    @Test
    public void testParse() {
        log.info("url encode: {}", UrlEncoded.encodeString("&="));

        var str = "name=Samuel";
        var res = HttpParamUtil.parse(str);

        log.info("name is: {}", res.get("name"));
        assertEquals("Samuel", res.get("name").get(0));

        str = "name=Sam&willing=false";
        res = HttpParamUtil.parse(str);
        assertEquals("Sam", res.get("name").get(0));
        assertEquals("false", res.get("willing").get(0));
    }
}
