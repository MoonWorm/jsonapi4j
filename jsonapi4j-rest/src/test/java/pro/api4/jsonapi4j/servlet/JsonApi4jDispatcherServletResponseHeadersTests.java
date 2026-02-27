package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonApi4jDispatcherServletResponseHeadersTests {

    @Test
    public void writeResponseBody_setsExactJsonApiContentTypeWithoutCharset() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();

        Field objectMapperField = JsonApi4jDispatcherServlet.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(sut, new ObjectMapper());

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());

        Method writeResponseBody = JsonApi4jDispatcherServlet.class.getDeclaredMethod(
                "writeResponseBody",
                HttpServletResponse.class,
                Object.class
        );
        writeResponseBody.setAccessible(true);
        writeResponseBody.invoke(sut, response, Map.of("ok", true));

        verify(response).setHeader("Content-Type", JsonApiMediaType.MEDIA_TYPE);
        verify(response, never()).setContentType(anyString());
        verify(response, never()).setCharacterEncoding(anyString());
    }

    private static class TestServletOutputStream extends ServletOutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // no-op for tests
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }
    }
}
