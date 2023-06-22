package cn.gzten.jabu.pojo;

import org.junit.jupiter.api.Test;

public class RequestMethodTests {
    @Test
    public void testSerializeArray() {
        var x = new RequestMethod[] {RequestMethod.GET, RequestMethod.POST};
        System.out.println(RequestMethod.serializeArray(x));
    }
}
