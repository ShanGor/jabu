package cn.gzten.pojo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

public class RequestMethodTests {
    @Test
    public void testSerializeArray() {
        var x = new RequestMethod[] {RequestMethod.GET, RequestMethod.POST};
        System.out.println(RequestMethod.serializeArray(x));
    }
}
