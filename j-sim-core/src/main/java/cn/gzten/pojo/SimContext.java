package cn.gzten.pojo;

import cn.gzten.util.SimUtils;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.io.ByteBufferInputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.UrlEncoded;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimContext {
    @Getter
    private Request request;
    @Getter
    private Response response;
    @Getter
    private Callback callback;

    @Setter
    private int downloadBufferSize = 4096;

    public SimContext(Request request, Response response, Callback callback) {
        this.request = request;
        this.response = response;
        this.callback = callback;
    }
    public String getPath() {
        return request.getHttpURI().getPath();
    }

    /**
     * The result could be empty, but never null
     * @return
     */
    public MultiMap<String> getQueryParams() {
        var query = request.getHttpURI().getQuery();
        if (StringUtil.isBlank(query)) return new MultiMap<>();
        return UrlEncoded.decodeQuery(query);
    }

    public ByteBufferInputStream getRequestBodyAsStream() {
        return new ByteBufferInputStream(request.read().getByteBuffer());
    }

    public SocketAddress getRemoteIp() {
        return request.getConnectionMetaData().getRemoteSocketAddress();
    }

    public SimContext addHeader(String key, String value) {
        response.getHeaders().add(key, value);
        return this;
    }

    public SimContext setContentType(String type) {
        return addHeader("Content-Type", type);
    }

    public SimContext withStatus(int status) {
        response.setStatus(status);
        return this;
    }

    public SimContext returnAsJson() {
        return setContentType("application/json");
    }

    public SimContext returnAsText() {
        return setContentType("text/plain");
    }

    public SimContext returnAsHtml() {
        return setContentType("text/html");
    }

    public void returnVoid() {
        callback.succeeded();
    }

    public void completeWithStatus(int statusCode) {
        withStatus(statusCode);
        response.write(true, null, callback);
    }

    /**
     * Write some bytes to the response, not yet completed.
     * @param bytes
     */
    public void write(byte[] bytes) {
        write(ByteBuffer.wrap(bytes));
    }

    /**
     * Write some bytes to the response, not yet completed.
     * @param bytes
     */
    public void write(byte[] bytes, int offset, int len) {
        write(ByteBuffer.wrap(bytes, offset, len));
    }

    /**
     * Serve a ByteChannel as a download file. Default buffer_size is 4096, please change it by ctx.setDownloadBufferSize(int)
     * @param channel
     * @param fileName
     * @throws IOException
     */
    public void serveByteChannelToDownload(ByteChannel channel, String fileName) throws IOException {
        SimUtils.serveChannelToDownload(channel, this, downloadBufferSize, fileName);
    }

    /**
     * Serve an input stream as a download file. Default buffer_size is 4096, please change it by ctx.setDownloadBufferSize(int)
     * @param ins
     * @param fileName
     * @throws IOException
     */
    public void serveInputStreamToDownload(InputStream ins, String fileName) throws IOException {
        SimUtils.serveInputStreamToDownload(ins, this, downloadBufferSize, fileName);
    }

    /**
     * Synchronized operation, only return while the write action completed.
     * @param buff
     */
    public void write(ByteBuffer buff) {
        var completed = new AtomicBoolean(false);
        response.write(false, buff, Callback.from(() -> completed.set(true)));
        while(!completed.get()) {
            Thread.onSpinWait();
        }
    }

    public void returnWith(int statusCode, String value) {
        withStatus(statusCode).returnWith(value);
    }

    public void returnWith(String value) {
        response.write(true, ByteBuffer.wrap(value.getBytes()), callback);
    }

    public void returnWith(int statusCode, String value, Map<String, String> headers) {
        if (headers != null) headers.forEach((k, v) -> addHeader(k, v));
        returnWith(statusCode, value);
    }
}
