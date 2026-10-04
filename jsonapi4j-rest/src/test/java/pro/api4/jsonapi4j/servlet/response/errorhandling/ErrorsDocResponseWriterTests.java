package pro.api4.jsonapi4j.servlet.response.errorhandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.errorhandling.ErrorsDocFactory;
import pro.api4.jsonapi4j.errorhandling.ErrorsDocSupplier;
import pro.api4.jsonapi4j.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErrorsDocResponseWriterTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
    private final ErrorsDocResponseWriter sut = new ErrorsDocResponseWriter(registry, MAPPER);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() throws IOException {
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                body.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }
        });
    }

    @Test
    public void write_mappedFailure_writesItsStatusAndJsonApiErrorDocument() throws IOException {
        registry.register(IllegalStateException.class, new ErrorsDocSupplier<IllegalStateException>() {
            @Override
            public ErrorsDoc getErrorResponse(IllegalStateException ex) {
                return ErrorsDocFactory.badGatewayErrorsDoc("downstream failed");
            }

            @Override
            public int getHttpStatus(IllegalStateException ex) {
                return 502;
            }
        });

        sut.write(response, new IllegalStateException("boom"));

        verify(response).setStatus(502);
        verify(response).setContentType("application/vnd.api+json");
        verify(response).setCharacterEncoding("UTF-8");
        JsonNode error = MAPPER.readTree(body.toByteArray()).path("errors").get(0);
        assertThat(error.path("code").asText()).isEqualTo("BAD_GATEWAY");
        assertThat(error.path("detail").asText()).isEqualTo("downstream failed");
    }

    @Test
    public void write_unmappedFailure_writesGenericInternalErrorWithoutItsMessage() throws IOException {
        sut.write(response, new IllegalStateException("secret internals"));

        verify(response).setStatus(500);
        assertThat(new String(body.toByteArray())).doesNotContain("secret internals");
    }

    @Test
    public void errorIds_errorsWithIds_joinsThemInOrder() {
        ErrorsDoc errorsDoc = new ErrorsDoc(List.of(
                ErrorObject.builder().id("a").build(),
                ErrorObject.builder().id("b").build()
        ));

        assertThat(ErrorsDocResponseWriter.errorIds(errorsDoc)).isEqualTo("a,b");
    }

    @Test
    public void errorIds_errorsWithoutIds_skipsThem() {
        ErrorsDoc errorsDoc = new ErrorsDoc(Arrays.asList(
                ErrorObject.builder().build(),
                null,
                ErrorObject.builder().id("b").build()
        ));

        assertThat(ErrorsDocResponseWriter.errorIds(errorsDoc)).isEqualTo("b");
    }

    @Test
    public void errorIds_nullDocOrErrors_returnsEmpty() {
        assertThat(ErrorsDocResponseWriter.errorIds(null)).isEmpty();
        assertThat(ErrorsDocResponseWriter.errorIds(new ErrorsDoc(null))).isEmpty();
    }

    @Test
    public void write_supplierWithoutIds_stillWritesErrorDocument() throws IOException {
        registry.register(IllegalStateException.class, new ErrorsDocSupplier<IllegalStateException>() {
            @Override
            public ErrorsDoc getErrorResponse(IllegalStateException ex) {
                return new ErrorsDoc(List.of(ErrorObject.builder().code("CUSTOM").build()));
            }

            @Override
            public int getHttpStatus(IllegalStateException ex) {
                return 409;
            }
        });

        sut.write(response, new IllegalStateException("boom"));

        verify(response).setStatus(409);
        JsonNode error = MAPPER.readTree(body.toByteArray()).path("errors").get(0);
        assertThat(error.path("code").asText()).isEqualTo("CUSTOM");
    }

}
