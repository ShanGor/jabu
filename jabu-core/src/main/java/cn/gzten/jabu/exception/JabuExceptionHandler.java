package cn.gzten.jabu.exception;

public interface JabuExceptionHandler {
    ExceptionHandleResponse handle(Exception e);
    class DefaultJabuExceptionHandler implements JabuExceptionHandler {
        @Override
        public ExceptionHandleResponse handle(Exception e) {
            var resp = new ExceptionHandleResponse();
            if (e instanceof JabuRequestError) {
                resp.setStatus(400);
            } else {
                resp.setStatus(500);
            }

            resp.setContentType("PLAIN/TEXT");
            resp.setBody(e.getMessage());
            return resp;
        }
    }
}
