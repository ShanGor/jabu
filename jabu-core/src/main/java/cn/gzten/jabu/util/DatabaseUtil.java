package cn.gzten.jabu.util;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class DatabaseUtil {
    public static <T> List<T> query(String sql, DataSource dataSource, Function<ResultSet, T> from) {
        List<T> list = new LinkedList<>();
        try(Connection conn=dataSource.getConnection()) {
            try(var stmt = conn.prepareStatement(sql)) {
                try(var rs = stmt.executeQuery()) {
                    while(rs.next()) {
                        list.add(from.apply(rs));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
