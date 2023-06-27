package cn.gzten.example.controller;

import cn.gzten.example.bean.SimpleBeans;
import cn.gzten.jabu.annotation.*;
import cn.gzten.jabu.core.JabuContext;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * With basePath of all APIs.
 */
@Controller(basePath = "/api")
@Data
public class AnotherController {

    @Inject
    @Setter
    HikariDataSource dataSource;

    @PostConstruct
    public void init(SimpleBeans.BeanC a) {
        System.out.println("Post construct: Haha, I am " + a.c);
    }

    @Setter
    private String hello;
    @Route(path = "/test-another")
    public String test() {
        return "Hello world";
    }

    @Route(path = "/test-download")
    public void testDownloadFile(JabuContext ctx) {
        var BUF_SIZE = 4096;
        try {
            var fileName = "ffmpeg-release-full.7z";
            var path = Paths.get("D:\\迅雷下载", fileName);
            ctx.setDownloadBufferSize(BUF_SIZE);

            ctx.serveByteChannelToDownload(Files.newByteChannel(path, StandardOpenOption.READ), fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Route(path = "/test-download-classpath")
    public void testDownloadClassPathFile(JabuContext ctx) {
        try {
            var fileName = "test-cases.txt";
            var ins = this.getClass().getClassLoader().getResourceAsStream(fileName);

            ctx.serveInputStreamToDownload(ins, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void preDestroy(SimpleBeans.BeanA a) {
        System.out.println("*** Now exiting! hello: " + a.a);
    }

    @PreDestroy
    public void preDestroyWithoutParam() {
        System.out.println("*** Exiting..");
    }
}
