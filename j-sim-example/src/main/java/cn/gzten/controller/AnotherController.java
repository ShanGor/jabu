package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;

@Controller
public class AnotherController {
    @RequestMapping(path = "/test-another")
    public String test() {
        return "Hello world";
    }
}
