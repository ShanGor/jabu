package cn.gzten.example.config;

import cn.gzten.jabu.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "example")
@Data
public class ExampleConfig {
    private String hello;

    private int world;
}
