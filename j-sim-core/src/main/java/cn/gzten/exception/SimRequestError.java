package cn.gzten.exception;

public class SimRequestError extends RuntimeException {
    public SimRequestError(String msg) {
        super(msg);
    }
}
