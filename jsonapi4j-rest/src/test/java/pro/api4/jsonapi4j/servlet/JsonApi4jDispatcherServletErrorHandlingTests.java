package pro.api4.jsonapi4j.servlet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonApi4jDispatcherServletErrorHandlingTests {

    @Test
    public void handledException_writesJsonApiErrorAndDoesNotRethrow() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();
        JsonApiRequestSupplier<HttpServletRequest> supplier = mock(JsonApiRequestSupplier.class);
        ErrorHandlerFactoriesRegistry errorHandlers = mock(ErrorHandlerFactoriesRegistry.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RuntimeException failure = new RuntimeException("boom");

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/jsonapi/users");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());
        when(supplier.from(request)).thenThrow(failure);
        when(errorHandlers.resolveStatusCode(failure)).thenReturn(400);
        when(errorHandlers.resolveErrorsDoc(failure)).thenReturn(new ErrorsDoc(List.of()));

        setField(sut, "jsonApiRequestSupplier", supplier);
        setField(sut, "errorHandlerFactory", errorHandlers);
        setField(sut, "objectMapper", new ObjectMapper());

        assertThatCode(() -> sut.service(request, response))
                .doesNotThrowAnyException();

        verify(response).setStatus(400);
        verify(errorHandlers).resolveErrorsDoc(failure);
    }

    @Test
    public void handled4xxException_logsWarnWithoutStackTrace() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();
        JsonApiRequestSupplier<HttpServletRequest> supplier = mock(JsonApiRequestSupplier.class);
        ErrorHandlerFactoriesRegistry errorHandlers = mock(ErrorHandlerFactoriesRegistry.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RuntimeException failure = new RuntimeException("invalid filter");

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/jsonapi/users");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());
        when(supplier.from(request)).thenThrow(failure);
        when(errorHandlers.resolveStatusCode(failure)).thenReturn(400);
        when(errorHandlers.resolveErrorsDoc(failure)).thenReturn(new ErrorsDoc(List.of()));

        setField(sut, "jsonApiRequestSupplier", supplier);
        setField(sut, "errorHandlerFactory", errorHandlers);
        setField(sut, "objectMapper", new ObjectMapper());

        Logger logger = (Logger) LoggerFactory.getLogger(JsonApi4jDispatcherServlet.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatCode(() -> sut.service(request, response))
                    .doesNotThrowAnyException();

            List<ILoggingEvent> clientErrorEvents = appender.list.stream()
                    .filter(event -> event.getFormattedMessage().contains("Handled JSON:API client error"))
                    .toList();

            assertThat(clientErrorEvents).hasSize(1);
            ILoggingEvent clientErrorEvent = clientErrorEvents.getFirst();
            assertThat(clientErrorEvent.getLevel()).isEqualTo(Level.WARN);
            assertThat(clientErrorEvent.getThrowableProxy()).isNull();
            assertThat(
                    appender.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains("Handled JSON:API client error"))
            ).isFalse();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void handled5xxException_logsErrorWithStackTrace() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();
        JsonApiRequestSupplier<HttpServletRequest> supplier = mock(JsonApiRequestSupplier.class);
        ErrorHandlerFactoriesRegistry errorHandlers = mock(ErrorHandlerFactoriesRegistry.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RuntimeException failure = new RuntimeException("downstream unavailable");

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/jsonapi/users");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());
        when(supplier.from(request)).thenThrow(failure);
        when(errorHandlers.resolveStatusCode(failure)).thenReturn(500);
        when(errorHandlers.resolveErrorsDoc(failure)).thenReturn(new ErrorsDoc(List.of()));

        setField(sut, "jsonApiRequestSupplier", supplier);
        setField(sut, "errorHandlerFactory", errorHandlers);
        setField(sut, "objectMapper", new ObjectMapper());

        Logger logger = (Logger) LoggerFactory.getLogger(JsonApi4jDispatcherServlet.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatCode(() -> sut.service(request, response))
                    .doesNotThrowAnyException();

            List<ILoggingEvent> serverErrorEvents = appender.list.stream()
                    .filter(event -> event.getFormattedMessage().contains("Handled JSON:API server error"))
                    .toList();

            assertThat(serverErrorEvents).hasSize(1);
            ILoggingEvent serverErrorEvent = serverErrorEvents.getFirst();
            assertThat(serverErrorEvent.getLevel()).isEqualTo(Level.ERROR);
            assertThat(serverErrorEvent.getThrowableProxy()).isNotNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static void setField(Object target,
                                 String fieldName,
                                 Object value) throws Exception {
        Field field = JsonApi4jDispatcherServlet.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
    }
}
