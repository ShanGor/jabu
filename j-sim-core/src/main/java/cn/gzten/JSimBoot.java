package cn.gzten;

import cn.gzten.exception.ExceptionHandleResponse;
import cn.gzten.exception.SimExceptionHandler;
import cn.gzten.sim.JSimEntry;
import cn.gzten.util.HttpParamUtil;
import cn.gzten.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import java.util.concurrent.Executors;

@Slf4j
public class JSimBoot {
    private JSimEntry simEntry;

    private SimExceptionHandler exceptionHandler = null;

    public JSimBoot(JSimEntry entry) {
        this.simEntry = entry;
        this.simEntry.init();
    }

    public JSimBoot setExceptionHandler(SimExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public void startServer() throws Exception {
        if (this.exceptionHandler == null) {
            this.exceptionHandler = new SimExceptionHandler.DefaultSimExceptionHandler();
        }

        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Create a Server instance.
        Server server = new Server(threadPool);

        // Create a ServerConnector to accept connections from clients.
        var connector = new ServerConnector(server, 189, 10, new HttpConnectionFactory());
        connector.setPort(8080);
        connector.setAcceptQueueSize(10000);

        // Add the Connector to the Server
        server.addConnector(connector);

        // Set a simple Handler to handle requests/responses.
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                log.info("IP is: {}", request.getConnectionMetaData().getRemoteSocketAddress());

                var path = request.getHttpURI().getPath();
                log.info("Requesting: {}",path);

                try {
                    simEntry.tryProcessRoute(request, response, callback);
                } catch (Exception e) {
                        var resp = exceptionHandler.handle(e);
                        response.getHeaders().add("Content-Type", resp.getContentType());
                        if (ExceptionHandleResponse.JSON.equals(resp.getContentType())) {
                            HttpParamUtil.returnError(response, callback,resp.getStatus(), JsonUtil.toJson(resp.getBody()));
                        } else {
                            HttpParamUtil.returnError(response, callback,resp.getStatus(), resp.getBody().toString());
                        }
                }

                return true;
            }
        });

        // Start the Server so it starts accepting connections from clients.
        server.start();
    }
}