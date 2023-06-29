package cn.gzten.jabu.util;

import cn.gzten.jabu.exception.JabuStartUpError;
import cn.gzten.jabu.core.JabuContext;
import cn.gzten.jabu.pojo.RequestMethod;
import com.google.gson.internal.LinkedTreeMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.*;

import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
public class JabuUtils {

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
     * Used to do @Inject
     * @param obj
     * @param fieldName
     * @param bean
     */
    public static void injectBean(Object obj, String fieldName, Object bean) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, bean);
        } catch (NoSuchFieldException e) {
            throw new JabuStartUpError("Failed at wiring beans: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new JabuStartUpError(e.getMessage());
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

    public static boolean matchPath(JabuContext ctx, String pathInRequest, String pathInRequestMapping, boolean regex) {
        if (regex) {
            return pathInRequest.matches(pathInRequestMapping);
        } else {
            if (pathInRequestMapping.contains("{") || pathInRequestMapping.contains("*")) {
                var newPattern = convertPathPatternTobeRegex(pathInRequestMapping);
                if (newPattern.isPresent()) {
                    var pathVariables = getPathVariables(newPattern, pathInRequest);
                    if (pathVariables.isEmpty()) return false;

                    ctx.setPathVariables(pathVariables);
                    return true;
                }
                return false;
            } else {
                return pathInRequestMapping.equals(pathInRequest);
            }
        }
    }

    /**
     * Serve a byte channel as a download file.
     * @param channel
     * @param ctx
     * @param BUF_SIZE
     * @param fileName
     * @throws IOException
     */
    public static void serveChannelToDownload(ByteChannel channel, JabuContext ctx, int BUF_SIZE, String fileName) throws IOException {
        var bf = ByteBuffer.allocate(BUF_SIZE);
        ctx.addHeader("Content-Disposition", "attachment; filename=\"%s\"".formatted(fileName));
        while(channel.read(bf) > 0) {
            if (bf.position() == BUF_SIZE) {
                bf.rewind();
                ctx.write(bf);
            } else {
                ctx.write(bf.array(), 0, bf.position());
            }
            bf.clear();
        }
        ctx.completeWithStatus(200);
    }

    /**
     * Serve an input stream as a download file.
     * @param ins
     * @param ctx
     * @param BUF_SIZE
     * @param fileName
     * @throws IOException
     */
    public static void serveInputStreamToDownload(InputStream ins, JabuContext ctx, int BUF_SIZE, String fileName) throws IOException {
        var bf = new byte[BUF_SIZE];
        ctx.addHeader("Content-Disposition", "attachment; filename=\"%s\"".formatted(fileName));
        int len = ins.read(bf);
        while(len > 0) {
            ctx.write(bf, 0, len);
            len = ins.read(bf);
        }
        ctx.completeWithStatus(200);
    }


    public static Optional<java.util.Map<String, String>> getPathVariables(String pattern, String endpoint) {
        return getPathVariables(convertPathPatternTobeRegex(pattern), endpoint);
    }

    public static Optional<java.util.Map<String, String>> getPathVariables(Optional<Map.Entry<String, List<String>>> regexPattern, String endpoint) {
        if (regexPattern.isPresent()) {
            log.debug(regexPattern.get().getKey());
            var map = new HashMap<String, String>();
            var m = Pattern.compile(regexPattern.get().getKey()).matcher(endpoint);
            if (m.matches()) {
                regexPattern.get().getValue().forEach(k -> {
                    map.put(k, m.group(k));
                });
            } else {
                return Optional.empty();
            }
            return Optional.of(map);
        }

        return Optional.empty();
    }

    /**
     * Convert a path variable pattern to be regex pattern, return along with the path variable names.
     * @param pattern
     * @return
     */
    public static final Optional<Map.Entry<String, List<String>>> convertPathPatternTobeRegex(final String pattern) {
        var p = Pattern.compile("(\\{(?<name>[a-zA-Z0-9_\\-]+)\\})");
        var m = p.matcher(pattern);

        var sb = new StringBuilder();
        var listOfPathVarNames = new LinkedList<String>();

        var lastPos = 0;
        while (m.find()) {
            var pathVarName = m.group("name");
            var startPos = m.start(1);
            var endPos = m.end(1);

            listOfPathVarNames.add(pathVarName);

            if (startPos > lastPos) {
                sb.append(pattern.substring(lastPos, startPos));
            }
            sb.append("(?<%s>[^/]+)".formatted(pathVarName));
            lastPos = endPos;
        }

        if (pattern.length() > lastPos) {
            sb.append(pattern.substring(lastPos));
        }

        return Optional.of(Map.entry(sb.toString()
                        .replace("/**", "/.*")
                        .replace("/*", "/[^/]*"),
                listOfPathVarNames));
    }

    /**
     * Shortcut to shutdown hook
     * @param runnable
     */
    public static final void addShutdownHook(final Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
    }

    public static final Optional<InputStream> getClasspathResource(String path) {
        try {
            return Optional.of(JabuUtils.class.getClassLoader().getResourceAsStream(path));
        } catch (RuntimeException e) {
            return Optional.empty();
        }

    }
}
