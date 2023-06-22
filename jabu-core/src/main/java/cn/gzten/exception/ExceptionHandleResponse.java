package cn.gzten.exception;

import lombok.Data;

@Data
public class ExceptionHandleResponse {
    public static final String JSON = "application/json";
    private int status;
    private Object body;
    private String contentType = JSON;
}
