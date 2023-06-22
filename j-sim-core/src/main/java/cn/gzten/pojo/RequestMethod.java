package cn.gzten.pojo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public enum RequestMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE;

    public static Optional<RequestMethod> resolve(String method) {
        if (method == null) return Optional.empty();

        switch (method.toUpperCase(Locale.ROOT)) {
            case "GET": return Optional.of(GET);
            case "HEAD": return Optional.of(HEAD);
            case "POST": return Optional.of(POST);
            case "PUT": return Optional.of(PUT);
            case "PATCH": return Optional.of(PATCH);
            case "DELETE": return Optional.of(DELETE);
            case "OPTIONS": return Optional.of(OPTIONS);
            case "TRACE": return Optional.of(TRACE);
            default: return Optional.empty();
        }
    }

    public static String serializeArray(RequestMethod[] methods) {
        return String.join(",", Arrays.stream(methods).map(method -> "RequestMethod.%s".formatted(method.name())).collect(Collectors.toList()));
    }
}