package cn.gzten.example.service;

import cn.gzten.example.bean.SimpleBeans;
import cn.gzten.jabu.annotation.Inject;
import cn.gzten.jabu.annotation.Service;
import lombok.Getter;
import lombok.Setter;

@Service
public class TestService {
    @Inject
    private SimpleBeans.Hello hello;

    @Inject
    @Setter
    @Getter
    private SimpleBeans.World world;

    @Inject(qualifier = "wide")
    @Setter
    private SimpleBeans.Wide wide;
}
