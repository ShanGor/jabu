package cn.gzten.util;

import cn.gzten.exception.SimStartUpError;
import cn.gzten.pojo.RequestMethod;
import org.eclipse.jetty.util.StringUtil;

import java.util.Locale;

public class SimUtils {

    /**
     * Set the first character to uppercase
     * @param str
     * @return
     */
    public static final String capitalize(final String str) {
        if (StringUtil.isBlank(str)) return str;

        if (str.length() == 1) {
            return str.toUpperCase(Locale.ROOT);
        }

        var c = str.charAt(0);
        if (c >= 'a' && c <= 'z') {
            StringBuilder sb = new StringBuilder();
            sb.append((char)(c + 'A' - 'a'));
            sb.append(str.substring(1));
            return sb.toString();
        } else {
            return str;
        }
    }

    /**
     * Set the first character to lowercase
     * @param str
     * @return
     */
    public static final String lowercaseInitial(final String str) {
        if (StringUtil.isBlank(str)) return str;

        if (str.length() == 1) {
            return str.toLowerCase(Locale.ROOT);
        }

        var c = str.charAt(0);
        if (c >= 'A' && c <= 'Z') {
            StringBuilder sb = new StringBuilder();
            sb.append((char)(c - 'A' + 'a'));
            sb.append(str.substring(1));
            return sb.toString();
        } else {
            return str;
        }
    }

    /**
     * Used to do @AutoWired
     * @param obj
     * @param fieldName
     * @param bean
     */
    public static void wireBean(Object obj, String fieldName, Object bean) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, bean);
        } catch (NoSuchFieldException e) {
            throw new SimStartUpError("Failed at wiring beans: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SimStartUpError(e.getMessage());
        }
    }

    public static final String composeSetter(final String fieldName) {
        return "set%s".formatted(capitalize(fieldName));
    }

    public static final boolean stringInArray(final String str, final String[] array) {
        if (StringUtil.isBlank(str)) return false;
        if (array == null || array.length == 0) return false;

        for (var s : array) {
            if (str.equals(s)) return true;
        }

        return false;
    }

    /**
     * Case-insensitive, and trim them all.
     * @param str
     * @param array
     * @return
     */
    public static final boolean stringInArrayCaseInsensitive(final String str, final String[] array) {
        if (StringUtil.isBlank(str)) return false;
        if (array == null || array.length == 0) return false;

        var t = str.trim().toLowerCase(Locale.ROOT);
        for (var s : array) {
            if (StringUtil.isBlank(s)) continue;

            if (t.equals(s.trim().toLowerCase(Locale.ROOT))) return true;
        }

        return false;
    }

    public static final boolean isEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }

    public static final boolean httpMethodMatches(RequestMethod method, RequestMethod[] methods) {
        for (var m : methods) {
            if (m.equals(method)) return true;
        }
        return false;
    }

    public static final boolean httpMethodMatches(String requestMethod, RequestMethod[] definedMethods) {
        var opt = RequestMethod.resolve(requestMethod);
        if (opt.isEmpty()) return false;
        var httpMethod = opt.get();
        if (definedMethods.length == 0) return true;

        for (var definedMethod : definedMethods) {
            if(definedMethod == httpMethod) return true;
        }

        return false;
    }

    public static boolean matchPath(String pathInRequest, String pathInRequestMapping, boolean regex) {
        if (regex) {
            return pathInRequest.matches(pathInRequestMapping);
        } else {
            if (pathInRequestMapping.contains("{") || pathInRequestMapping.contains("*")) {
                //TODO to implement
                return false;
            } else {
                return pathInRequestMapping.equals(pathInRequest);
            }
        }
    }
}
