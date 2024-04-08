package cn.gzten.example.config;

import cn.gzten.jabu.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "dataSource")
@Data
public class DataSourceConfig {
    private String jdbcUrl;
    private String user;
    private String password;
}
