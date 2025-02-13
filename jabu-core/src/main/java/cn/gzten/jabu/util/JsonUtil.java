package cn.gzten.jabu.util;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static <T> T toObject(String str, Class<T> clazz) {
        return gson.fromJson(str, clazz);
    }
    public static <T> T toObject(InputStream ins, Class<T> clazz) {
        return gson.fromJson(new InputStreamReader(ins), clazz);
    }

    public static <T> T toObject(InputStream ins, Type type) {
        return gson.fromJson(new InputStreamReader(ins), type);
    }

    public static <T> T toObject(InputStream ins, TypeToken<T> type) {
        return gson.fromJson(new InputStreamReader(ins), type);
    }
    public static <T> T toObject(String str, TypeToken<T> type) {
        return gson.fromJson(str, type);
    }
}
