package cn.gzten.jabu.exception;

public class JabuRequestError extends RuntimeException {
    public JabuRequestError(String msg) {
        super(msg);
    }
}
