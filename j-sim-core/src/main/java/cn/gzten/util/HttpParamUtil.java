package cn.gzten.util;

import cn.gzten.annotation.PathVar;
import cn.gzten.annotation.QueryParam;
import cn.gzten.annotation.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.ByteBufferInputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;


@Slf4j
public class HttpParamUtil {
    public static MultiMap<String> parse(String queryStr) {
        return UrlEncoded.decodeQuery(queryStr);
    }

    public static void returnError(Response response, Callback callback, int statusCode, String errorMessage) {
        response.setStatus(statusCode);
        response.write(true, ByteBuffer.wrap(errorMessage.getBytes()), callback);
    }

    public static boolean tryQueryParameter(Response response, Callback callback, Parameter methodParam, MultiMap<String> queryParams, Object[] params, int i) {
        var an = methodParam.getDeclaredAnnotation(QueryParam.class);
        if (an != null) {
            var value = queryParams.get(an.value());
            if (value == null || value.isEmpty()) {
                if (an.required()) {
                    returnError(response, callback, 400, "Missing parameter: %s".formatted(an.value()));
                } else {
                    params[i] = null;
                }
            }
            try {
                params[i] = doTypeMapping(methodParam, value.get(0));
            } catch (RuntimeException e) {
                returnError(response, callback, 400, "Parameter error: %s".formatted(e.getMessage()));
            }

            return true;
        }
        return false;
    }

    public static boolean tryPathVariable(Response response, Callback callback, Parameter methodParam, MultiMap<String> queryParams, Object[] params, int i) {
        var an = methodParam.getDeclaredAnnotation(PathVar.class);
        if (an != null) {
            var value = queryParams.get(an.value());
            if (value == null || value.isEmpty()) {
                returnError(response, callback, 400, "Missing path variable: %s".formatted(an.value()));
            }
            try {
                params[i] = doTypeMapping(methodParam, value.get(0));
            } catch (RuntimeException e) {
                returnError(response, callback, 400, "Path variable error: %s".formatted(e.getMessage()));
            }

            return true;
        }
        return false;
    }
    public static boolean tryRequestBody(Request request, Response response, Callback callback, Parameter methodParam, Object[] params, int i) {
        var an = methodParam.getDeclaredAnnotation(RequestBody.class);
        if (an != null) {
            try(var ins = new ByteBufferInputStream(request.read().getByteBuffer())) {
                if (methodParam.getType() == String.class) {
                    params[i] = new String(ins.readAllBytes());
                } else if (methodParam.getType() == Integer.class) {
                    params[i] = Integer.parseInt(new String(ins.readAllBytes()));
                } else if (methodParam.getType() == Long.class) {
                    params[i] = Long.parseLong(new String(ins.readAllBytes()));
                } else {
                    params[i] = JsonUtil.toObject(ins, methodParam.getType());
                }
            } catch (RuntimeException|IOException e) {
                e.printStackTrace();
                returnError(response, callback, 400, "Request body parsing error: %s".formatted(e.getMessage()));
            }

            return true;
        }

        return false;
    }

//    public static <T> T tryRequestBody(Request request, Response response, Callback callback, TypeToken<T> type) {
//
//        try(var ins = new ByteBufferInputStream(request.read().getByteBuffer())) {
//            if (type.getRawType() == String.class) {
//                return (T)new String(ins.readAllBytes());
//            } else if (type.getRawType() == Integer.class) {
//                return (T)(Integer)Integer.parseInt(new String(ins.readAllBytes()));
//            } else if (type.getRawType() == Long.class) {
//                return (T)(Long)Long.parseLong(new String(ins.readAllBytes()));
//            } else {
//                return JsonUtil.toObject(ins, type);
//            }
//        } catch (RuntimeException|IOException e) {
//            e.printStackTrace();
//            returnError(response, callback, 400, "Request body parsing error: %s".formatted(e.getMessage()));
//        }
//    }

    public static Object doTypeMapping(Parameter methodParam, String value) {
        if (methodParam.getType() == String.class) {
            return value;
        } else if (methodParam.getType() == Integer.class) {
            return Integer.parseInt(value);
        } else if (methodParam.getType() == Long.class) {
            return Long.parseLong(value);
        }
        return value;
    }
}
