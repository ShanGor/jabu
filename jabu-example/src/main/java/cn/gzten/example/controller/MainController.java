package cn.gzten.example.controller;

import cn.gzten.example.bean.SimpleBeans;
import cn.gzten.example.data.MyTable;
import cn.gzten.example.data.MyTableRepository;
import cn.gzten.example.service.TestService;
import cn.gzten.jabu.annotation.*;
import cn.gzten.jabu.pojo.RequestMethod;
import lombok.Data;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    @Inject
    @Setter
    TestService testService;

    @Prop("example.hello")
    @Setter
    String hello;

    @Prop("example.world")
    int helloWorld;

    @Prop("example.belovedBooks")
    String[] belovedBooks;

    /**
     * Please do not use List<String> but only List here, otherwise will cause compilation failure.
     */
    @Prop("example.belovedBooks")
    List listOfBelovedBooks;

    @Route(path = "/users", method = RequestMethod.GET)
    public List<MyTable> getUsers() {
        var li = MyTableRepository.findAll();
        return li;
    }

    @Route(path = "/hello", method = RequestMethod.GET)
    public String hello(@QueryParam("name") String nameForHello) {
        return "Hello, %s".formatted(nameForHello);
    }

    @Route(path = "/test-void", method = RequestMethod.GET)
    public void testVoid(@QueryParam String name) {
        System.out.println(name);
    }

    @Route(path = "/world", method = RequestMethod.GET)
    public Map<String, String> test() {
        return Map.of("Hey", "you");
    }

    @Route(path = "/test-int", method = RequestMethod.GET)
    public int testInt() {
        return helloWorld;
    }

    @Route(path = "/test-integer", method = RequestMethod.GET)
    public Integer testInteger() {
        return 128;
    }

    @Route(path = "/test/{year}/{month}", method = RequestMethod.GET)
    public String testYearMonth(@PathVar Integer year, @PathVar("month") Integer myMonth) {
        return "%d-%d".formatted(year, myMonth);
    }

    @Route(path = "/test-bool", method = RequestMethod.GET)
    public boolean testBool() {
        return true;
    }

    @Route(path = "/test-beloved-books", method = RequestMethod.GET)
    public String[] getBelovedBooks() {
        return belovedBooks;
    }
    @Route(path = "/test-list-beloved-books", method = RequestMethod.GET)
    public List<String> getListOfBelovedBooks() {
        return listOfBelovedBooks;
    }

    @Route(path = "/test", method = RequestMethod.POST)
    public String postHey(@RequestBody List<Hey> hey) {
        return "Greeting from %s".formatted(hey.get(0).hey);
    }

    @Route(path = "/test-inject")
    public SimpleBeans.World testInject() {
        return testService.getWorld();
    }

    @Route(path = "/test-prop")
    public String testProp() {
        return hello;
    }

    @Route(path = "/test-inject-1")
    public SimpleBeans.BeanA testInject1(SimpleBeans.BeanA a) {
        return a;
    }

    @Data
    public static class Hey {
        private String hey;
    }
}
