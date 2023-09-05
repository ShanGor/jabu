package cn.gzten.jabu.util;

import org.eclipse.jetty.util.StringUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    public static boolean isBoolean(Class target) {
        return target.equals(Boolean.class) || target.equals(boolean.class);
    }

    /**
     * Given an object, convert to a target basic type;
     * @param o
     * @param target
     * @return
     */
    public static Object convertBasicTypes(Object o, Class target) {

        if (o instanceof Boolean) {
            if (isBoolean(target)) {
                return o;
            }
            if (target.equals(String.class)) {
                return o.toString();
            }
        } else if (o instanceof String) {
            if (target.equals(String.class)) {
                return o;
            }

            if (o == null || StringUtil.isBlank((String) o)) return null;

            if (TypeUtils.isLong(target))
                return Long.parseLong((String) o);
            if (TypeUtils.isInt(target))
                return Integer.parseInt((String) o);
            if (TypeUtils.isShort(target))
                return Short.parseShort((String) o);
            if (TypeUtils.isByte(target))
                return Byte.parseByte((String) o);
            if (isBoolean(target)) {
                return Boolean.parseBoolean((String) o);
            }
            if (TypeUtils.isDouble(target)){
                return Double.parseDouble((String) o);
            }
            if (TypeUtils.isFloat(target)){
                return Double.parseDouble((String) o);
            }
            if (target.equals(BigDecimal.class)) {
                return new BigDecimal((String)o);
            }

            if (target.equals(BigInteger.class)) {
                return new BigInteger((String)o);
            }
        } else if (o instanceof Number) {
            if (target.equals(String.class)) {
                return o.toString();
            }
            if (isLong(target)) {
                return ((Number) o).longValue();
            }
            if (isInt(target)) {
                return ((Number) o).intValue();
            }
            if (isShort(target)) {
                return ((Number) o).shortValue();
            }
            if (isByte(target)) {
                return ((Number) o).byteValue();
            }
            if (isDouble(target)) {
                return ((Number) o).doubleValue();
            }
            if (isFloat(target)) {
                return ((Number) o).floatValue();
            }
            if (target.equals(BigDecimal.class)) {
                return new BigDecimal(((Number) o).doubleValue());
            }
            if (target.equals(BigInteger.class)) {
                return BigInteger.valueOf(((Number) o).longValue());
            }
        }


        return o;
    }
}
