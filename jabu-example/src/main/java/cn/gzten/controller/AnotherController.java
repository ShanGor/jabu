package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;
import cn.gzten.pojo.SimContext;
import lombok.Data;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Controller
@Data
public class AnotherController {
    @Setter
    private String hello;
    @RequestMapping(path = "/test-another")
    public String test() {
        return "Hello world";
    }

    @RequestMapping(path = "/test-download")
    public void testDownloadFile(SimContext ctx) {
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
    @RequestMapping(path = "/test-download-classpath")
    public void testDownloadClassPathFile(SimContext ctx) {
        try {
            var fileName = "test-cases.txt";
            var ins = this.getClass().getClassLoader().getResourceAsStream(fileName);

            ctx.serveInputStreamToDownload(ins, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
