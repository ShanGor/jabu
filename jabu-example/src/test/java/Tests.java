import cn.gzten.example.config.ExampleConfig;
import cn.gzten.jabu.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class Tests {
    String baseUri = "http://localhost:8080";
    HttpClient httpClient = HttpClient.newHttpClient();
    @Test
    public void testInject1() throws IOException, InterruptedException {
        var endpoint = "/test-inject-1";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("""
                {"a":"Samuel, I am a special String"}""", resp.body());
    }

    @Test
    public void testInject() throws IOException, InterruptedException {
        var endpoint = "/test-inject";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("""
                {"name":"Samuel"}""", resp.body());
    }

    @Test
    public void testYearMonth() throws IOException, InterruptedException {
        var endpoint = "/test/2023/5";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("2023-5", resp.body());
    }

    @Test
    public void test() throws IOException, InterruptedException {
        var endpoint = "/test";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUri + endpoint))
                .POST(HttpRequest.BodyPublishers.ofString("[{\"hey\":\"you you\"}]"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("Greeting from you you", resp.body());
    }

    @Test
    public void testInteger() throws IOException, InterruptedException {
        var endpoint = "/test-integer";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("128", resp.body());
    }
    @Test
    public void testInt() throws IOException, InterruptedException {
        var endpoint = "/test-int";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("128", resp.body());
    }
    @Test
    public void testBool() throws IOException, InterruptedException {
        var endpoint = "/test-bool";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("true", resp.body());
    }
    @Test
    public void testHello() throws IOException, InterruptedException {
        var endpoint = "/hello?name=Samuel";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("Hello, Samuel", resp.body());
    }
    @Test
    public void testWorld() throws IOException, InterruptedException {
        var endpoint = "/world";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("""
                {"Hey":"you"}""", resp.body());
    }
    @Test
    public void testVoid() throws IOException, InterruptedException {
        var endpoint = "/test-void";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        endpoint = "/test-void?name=sam";resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("", resp.body());
    }

    @Test
    public void testAnother() throws IOException, InterruptedException {
        var endpoint = "/api/test-another";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("Hello world", resp.body());
    }

    @Test
    public void testDownloadClassPathFile() throws IOException, InterruptedException {
        var endpoint = "/api/test-download-classpath";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        var bf = this.getClass().getClassLoader().getResourceAsStream("test-cases.txt").readAllBytes();
        assertEquals(new String(bf), resp.body());

    }

    @Test
    public void testExampleConfig() throws IOException, InterruptedException {
        var endpoint = "/api/example-config";
        var resp = httpClient.send(HttpRequest.newBuilder()
                .GET().uri(URI.create(baseUri + endpoint))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        var config = JsonUtil.toObject(resp.body(), ExampleConfig.class);
        assertEquals("are you okay", config.getHello());
        assertEquals(404, config.getWorld());
    }
}
