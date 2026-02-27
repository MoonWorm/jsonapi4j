package pro.api4.jsonapi4j.servlet.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.exception.NotAcceptableException;
import pro.api4.jsonapi4j.http.exception.UnsupportedMediaTypeException;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpServletRequestJsonApiRequestSupplierMediaNegotiationTests {

    @Test
    public void strictMode_invalidJsonApiContentTypeParameter_returns415() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.CREATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json");
        when(request.getMethod()).thenReturn("POST");
        when(request.getInputStream()).thenReturn(inputStream("{}"));
        when(request.getContentType()).thenReturn("application/vnd.api+json;charset=UTF-8");
        when(request.getPathInfo()).thenReturn("/users");

        assertThatThrownBy(() -> sut.from(request)).isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    public void strictMode_unquotedProfileContentTypeValue_returns415() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.CREATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json");
        when(request.getMethod()).thenReturn("POST");
        when(request.getInputStream()).thenReturn(inputStream("{}"));
        when(request.getContentType()).thenReturn("application/vnd.api+json;profile=https://example.com/p");
        when(request.getPathInfo()).thenReturn("/users");

        assertThatThrownBy(() -> sut.from(request)).isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    public void strictMode_invalidJsonApiAcceptParameter_returns406() {
        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                mock(OperationDetailsResolver.class),
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json;charset=UTF-8");

        assertThatThrownBy(() -> sut.from(request)).isInstanceOf(NotAcceptableException.class);
    }

    @Test
    public void strictMode_nonUriExtAcceptParameter_returns406() {
        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                mock(OperationDetailsResolver.class),
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json;ext=\"foo\"");

        assertThatThrownBy(() -> sut.from(request)).isInstanceOf(NotAcceptableException.class);
    }

    @Test
    public void legacyMode_allowsTolerantMediaTypeParameters() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.CREATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.LEGACY
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json;charset=UTF-8");
        when(request.getMethod()).thenReturn("POST");
        when(request.getInputStream()).thenReturn(inputStream("{}"));
        when(request.getContentType()).thenReturn("application/vnd.api+json;charset=UTF-8");
        when(request.getPathInfo()).thenReturn("/users");
        when(request.getParameterMap()).thenReturn(Map.of());

        JsonApiRequest jsonApiRequest = sut.from(request);

        assertThat(jsonApiRequest).isNotNull();
        assertThat(jsonApiRequest.getOperationType()).isEqualTo(OperationType.CREATE_RESOURCE);
        assertThat(jsonApiRequest.getTargetResourceType()).isEqualTo(new ResourceType("users"));
    }

    private static ServletInputStream inputStream(String payload) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op for tests
            }
        };
    }
}
