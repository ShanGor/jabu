package cn.gzten.jabu.exception;

public class JabuException extends RuntimeException{
    public JabuException(Throwable e) {
        super(e);
    }

    public JabuException(String msg) {
        super(msg);
    }
}
