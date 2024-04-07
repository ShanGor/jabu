package cn.gzten.example.data;

import cn.gzten.jabu.annotation.Repository;
import lombok.Data;

@Repository(table = "my_table")
@Data
public class MyTable {
    private String username;
    private java.util.Date refDate;
}
