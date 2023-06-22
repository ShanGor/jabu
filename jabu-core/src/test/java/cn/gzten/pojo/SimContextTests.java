package cn.gzten.pojo;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class SimContextTests {
    @Test
    public void testGetQueryParams() {
        var request = mock(Request.class);
        var response = mock(Response.class);
        var callback = mock(Callback.class);
        var uri = mock(org.eclipse.jetty.http.HttpURI.class);
        var ctx = new SimContext(request, response, callback);

        when(request.getHttpURI()).thenReturn(uri);
        when(uri.getQuery()).thenReturn("name=Samuel");

        log.info("url encode: {}", UrlEncoded.encodeString("&="));

        var res = ctx.getQueryParams();

        log.info("name is: {}", res.get("name"));
        assertEquals("Samuel", res.get("name").get(0));

        when(uri.getQuery()).thenReturn("name=Sam&willing=false");
        res = ctx.getQueryParams();
        assertEquals("Sam", res.get("name").get(0));
        assertEquals("false", res.get("willing").get(0));
    }
}
