package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;
import cn.gzten.pojo.SimContext;
import lombok.Data;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
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
            var c = Files.newByteChannel(Paths.get("D:\\迅雷下载", "ffmpeg-release-full.7z"), StandardOpenOption.READ);
            var bf = ByteBuffer.allocate(BUF_SIZE);
            ctx.addHeader("Content-Disposition", "attachment; filename=\"Lets.go\"");
            while(c.read(bf) > 0) {
                if (bf.position() == BUF_SIZE) {
                    bf.rewind();
                    ctx.write(bf);
                } else {
                    ctx.write(bf.array(), 0, bf.position());
                }
                bf.clear();
            }
            ctx.completeWithStatus(200);

//            var c = Files.newInputStream(Paths.get("D:\\迅雷下载", "ffmpeg-release-full.7z"), StandardOpenOption.READ);
//            var bf = new byte[BUF_SIZE];
//            ctx.addHeader("Content-Disposition", "attachment; filename=\"Lets1.go\"");
//            int len = c.read(bf);
//            while(len > 0) {
//                ctx.incompleteReturn(bf, 0, len);
//                len = c.read(bf);
//            }
//            ctx.completeWithStatus(200);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
