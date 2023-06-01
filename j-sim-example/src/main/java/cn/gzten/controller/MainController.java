package cn.gzten.controller;

import cn.gzten.annotation.Controller;
import cn.gzten.annotation.QueryParam;
import cn.gzten.annotation.RequestBody;
import cn.gzten.annotation.RequestMapping;
import cn.gzten.pojo.RequestMethod;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    @RequestMapping(path = "/hello", method = RequestMethod.GET)
    public String hello(@QueryParam("name") String name) {
        return "Hello, %s".formatted(name);
    }

    @RequestMapping(path = "/test-void", method = RequestMethod.GET)
    public void testVoid(@QueryParam("name") String name) {
        System.out.println(name);
    }

    @RequestMapping(path = "/world", method = RequestMethod.GET)
    public Map<String, String> test() {
        return Map.of("Hey", "you");
    }

    @RequestMapping(path = "/test-int", method = RequestMethod.GET)
    public int testInt() {
        return 128;
    }

    @RequestMapping(path = "/test-integer", method = RequestMethod.GET)
    public Integer testInteger() {
        return 128;
    }

    @RequestMapping(path = "/test-bool", method = RequestMethod.GET)
    public boolean testBool() {
        return true;
    }

    @RequestMapping(path = "/test", method = RequestMethod.POST)
    public String postHey(@RequestBody List<Hey> hey) {
        return "Greeting from %s".formatted(hey.get(0).hey);
    }

    @Data
    public static class Hey {
        private String hey;
    }
}
