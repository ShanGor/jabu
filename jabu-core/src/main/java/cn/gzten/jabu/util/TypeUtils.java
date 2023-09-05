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
            return convertBasicTypesFromString((String) o, target);
        } else if (o instanceof Number) {
            return convertBasicTypesFromNumber((Number) o, target);
        }

        return o;
    }

    public static Object convertBasicTypesFromString(String o, Class target) {
        if (target.equals(String.class)) {
            return o;
        }

        if (o == null || StringUtil.isBlank(o)) return null;

        if (TypeUtils.isLong(target))
            return Long.parseLong(o);
        if (TypeUtils.isInt(target))
            return Integer.parseInt(o);
        if (TypeUtils.isShort(target))
            return Short.parseShort(o);
        if (TypeUtils.isByte(target))
            return Byte.parseByte(o);
        if (isBoolean(target)) {
            return Boolean.parseBoolean(o);
        }
        if (TypeUtils.isDouble(target)){
            return Double.parseDouble(o);
        }
        if (TypeUtils.isFloat(target)){
            return Double.parseDouble(o);
        }
        if (target.equals(BigDecimal.class)) {
            return new BigDecimal(o);
        }

        if (target.equals(BigInteger.class)) {
            return new BigInteger(o);
        }

        return o;
    }
    public static Object convertBasicTypesFromNumber(Number o, Class target) {
        if (target.equals(String.class)) {
            return o.toString();
        }
        if (isLong(target)) {
            return o.longValue();
        }
        if (isInt(target)) {
            return o.intValue();
        }
        if (isShort(target)) {
            return o.shortValue();
        }
        if (isByte(target)) {
            return o.byteValue();
        }
        if (isDouble(target)) {
            return o.doubleValue();
        }
        if (isFloat(target)) {
            return o.floatValue();
        }
        if (target.equals(BigDecimal.class)) {
            return new BigDecimal(o.doubleValue());
        }
        if (target.equals(BigInteger.class)) {
            return BigInteger.valueOf(o.longValue());
        }

        return o;
    }

}
