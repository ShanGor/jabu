package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;
import lombok.Data;
import lombok.Setter;

@Controller
@Data
public class AnotherController {
    @Setter
    private String hello;
    @RequestMapping(path = "/test-another")
    public String test() {
        return "Hello world";
    }
}
