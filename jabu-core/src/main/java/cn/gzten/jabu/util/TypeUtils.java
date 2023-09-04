package cn.gzten.jabu.util;

public class TypeUtils {
    public static boolean isLong(Class target) {
        return target.equals(long.class) || target.equals(Long.class);
    }

    public static boolean isInt(Class target) {
        return target.equals(int.class) || target.equals(Integer.class);
    }
    public static boolean isShort (Class target) {
        return target.equals(short.class) || target.equals(Short.class);
    }
    public static boolean isByte (Class target) {
        return target.equals(byte.class) || target.equals(Byte.class);
    }
    public static boolean isDouble (Class target) {
        return target.equals(Double.class) || target.equals(double.class);
    }
    public static boolean isFloat (Class target) {
        return target.equals(Float.class) || target.equals(float.class);
    }
}
