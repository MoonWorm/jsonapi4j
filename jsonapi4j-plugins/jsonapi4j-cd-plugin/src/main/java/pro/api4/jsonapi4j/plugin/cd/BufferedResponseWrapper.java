package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static pro.api4.jsonapi4j.http.HttpHeaders.CONTENT_LENGTH;

/**
 * Captures the body written by the rest of the chain instead of sending it, so the filter can rewrite it before it
 * reaches the client.
 *
 * <p>Everything that would describe or commit the original body on the wrapped response is kept here instead: the
 * content length - which describes the original body, not the rewritten one - and flushing, which would commit the
 * response before the filter sets its own headers. Text written through {@link #getWriter()} is encoded as UTF-8, the
 * encoding of JSON.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
    private final ServletOutputStream outputStream = new ServletOutputStream() {
        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            buffer.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException("Non-blocking writes are not supported while the body is captured");
        }
    };

    public BufferedResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * @return the captured body, including text still pending in the writer
     */
    public byte[] getCapturedBody() {
        writer.flush();
        return buffer.toByteArray();
    }

    /**
     * @return the captured body decoded as UTF-8
     */
    public String getCapturedBodyAsString() {
        return new String(getCapturedBody(), StandardCharsets.UTF_8);
    }

    @Override
    public void flushBuffer() {
        writer.flush();
    }

    @Override
    public void resetBuffer() {
        writer.flush();
        buffer.reset();
    }

    @Override
    public void reset() {
        super.reset();
        resetBuffer();
    }

    @Override
    public void setContentLength(int len) {
    }

    @Override
    public void setContentLengthLong(long len) {
    }

    @Override
    public void setHeader(String name, String value) {
        if (!isContentLength(name)) {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (!isContentLength(name)) {
            super.addHeader(name, value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (!isContentLength(name)) {
            super.setIntHeader(name, value);
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (!isContentLength(name)) {
            super.addIntHeader(name, value);
        }
    }

    private static boolean isContentLength(String name) {
        return CONTENT_LENGTH.getName().equalsIgnoreCase(name);
    }

}
