package cn.gzten.exception;

public interface SimExceptionHandler {
    ExceptionHandleResponse handle(Exception e);
    class DefaultSimExceptionHandler implements SimExceptionHandler {
        @Override
        public ExceptionHandleResponse handle(Exception e) {
            var resp = new ExceptionHandleResponse();
            if (e instanceof SimRequestError) {
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
