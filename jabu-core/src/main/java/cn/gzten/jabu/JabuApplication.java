package cn.gzten.jabu;

import cn.gzten.jabu.exception.ExceptionHandleResponse;
import cn.gzten.jabu.exception.JabuExceptionHandler;
import cn.gzten.jabu.core.JabuContext;
import cn.gzten.jabu.util.JabuUtils;
import cn.gzten.jabu.util.JsonPop;
import cn.gzten.jabu.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.util.concurrent.Executors;

@Slf4j
public class JabuApplication {
    private JabuEntry jabuEntry;

    private JabuExceptionHandler exceptionHandler = null;

    public JabuApplication(JabuEntry entry) {
        this.jabuEntry = entry;

        var props = new JsonPop();
        props.put("server.port", 8080);

        loadApplicationProperties("application.properties", props);

        var activeProfile = System.getProperty("jabu.profiles.active", null);
        if (activeProfile != null) {
            loadApplicationProperties("application-%s.properties".formatted(activeProfile), props);
        }
        this.jabuEntry.setProperties(props);

        this.jabuEntry.init();
    }

    private static void loadApplicationProperties(String path, JsonPop props) {
        var opt = JabuUtils.getClasspathResource(path);
        if (opt.isPresent()) {
            try (var ins = opt.get()) {

                props.load(ins);
            } catch (IOException e) {
                log.error("Failed to load properties {}: {}", path, e.getMessage());
            }
        } else {
            log.warn("No {} found!", path);
        }
    }

    public JabuApplication setExceptionHandler(JabuExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public void startServer() throws Exception {
        if (this.exceptionHandler == null) {
            this.exceptionHandler = new JabuExceptionHandler.DefaultJabuExceptionHandler();
        }

        // Create and configure a ThreadPool.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("server");
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Create a Server instance.
        Server server = new Server(threadPool);

        // Create a ServerConnector to accept connections from clients.
        var connector = new ServerConnector(server, 10, 5, new HttpConnectionFactory());
        connector.setPort(jabuEntry.properties.getProperties("server.port", Integer.class));
        connector.setAcceptQueueSize(10000);

        // Add the Connector to the Server
        server.addConnector(connector);

        // Set a simple Handler to handle requests/responses.
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                var ctx = new JabuContext(request, response, callback);
                log.info("IP is: {}", ctx.getRemoteIp());

                var path = ctx.getPath();
                log.info("Requesting: {}",path);

                try {
                    jabuEntry.tryProcessRoute(ctx);
                } catch (Exception e) {
                        var resp = exceptionHandler.handle(e);
                        ctx.setContentType(resp.getContentType());

                        if (ExceptionHandleResponse.JSON.equals(resp.getContentType())) {
                            ctx.returnWith(resp.getStatus(), JsonUtil.toJson(resp.getBody()));
                        } else {
                            ctx.returnWith(resp.getStatus(), resp.getBody().toString());
                        }
                }

                return true;
            }
        });

        // Start the Server so it starts accepting connections from clients.
        server.start();
    }
}