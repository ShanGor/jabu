package cn.gzten.jabu.util;

import cn.gzten.jabu.exception.JabuException;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class DatabaseUtil {
    public static <T> List<T> query(String sql, DataSource dataSource, Function<ResultSet, T> from) {
        return query(sql, null, dataSource, from);
    }

    public static <T> List<T> query(String sql, Object[] params, DataSource dataSource, Function<ResultSet, T> from) {
        List<T> list = new LinkedList<>();
        try(Connection conn=dataSource.getConnection()) {
            if (conn == null) {
                throw new JabuException("Can not get connection from dataSource");
            }
            conn.setAutoCommit(false);
            conn.setReadOnly(true);
            log.info("DB scheme is: {}", conn.getSchema());
            try(var stmt = conn.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        var obj = params[i];
                        if (obj == null) {
                            throw new JabuException("Query params[" + i + "] is null");
                        }
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                try(var rs = stmt.executeQuery()) {
                    while(rs.next()) {
                        list.add(from.apply(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new JabuException(e);
        }
        return list;
    }
}
