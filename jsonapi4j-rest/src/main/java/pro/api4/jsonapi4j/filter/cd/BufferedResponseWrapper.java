package pro.api4.jsonapi4j.filter.cd;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class BufferedResponseWrapper extends HttpServletResponseWrapper implements AutoCloseable {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
    private final ServletOutputStream outputStream = new ServletOutputStream() {
        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Not implemented
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

    public String getCaptureAsString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws Exception {
        writer.close();
        buffer.close();
        outputStream.close();
    }
}
