package cn.gzten.pojo;

import lombok.Getter;

public class ResponseEntity <T> {
    @Getter
    private T body;
    private int status;
    public static <T> ResponseEntity<T> ok(T body) {
        var o = new ResponseEntity<T>();
        o.body = body;
        return o;
    }
    public static <T> Builder<T> status(int value) {
        return new Builder(value);
    }

    public static class Builder<T> {
        private ResponseEntity o;
        public Builder(int value) {
            o = new ResponseEntity<T>();
            o.status = value;
        }

        public ResponseEntity<T> body(T t) {
            o.body = t;
            return o;
        }
    }
}
