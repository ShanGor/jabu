package cn.gzten.jabu.util;

public class DateTimeUtils {
    public static final java.util.Date fromDate(java.sql.Date date) {
        return new java.util.Date(date.getTime());
    }
}
