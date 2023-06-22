package cn.gzten.controller;

import cn.gzten.jabu.annotation.Controller;
import cn.gzten.jabu.annotation.QueryParam;
import cn.gzten.jabu.annotation.RequestBody;
import cn.gzten.jabu.annotation.RequestMapping;
import cn.gzten.jabu.pojo.RequestMethod;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    @RequestMapping(path = "/hello", method = RequestMethod.GET)
    public String hello(@QueryParam("name") String nameForHello) {
        return "Hello, %s".formatted(nameForHello);
    }

    @RequestMapping(path = "/test-void", method = RequestMethod.GET)
    public void testVoid(@QueryParam String name) {
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
