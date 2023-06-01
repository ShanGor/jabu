package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.RequestMapping;
import cn.gzten.pojo.RequestMethod;

@Controller
public class AnotherController {
    @RequestMapping(path = "/test-another", method = {RequestMethod.GET})
    public String test() {
        return "Hello world";
    }
}
