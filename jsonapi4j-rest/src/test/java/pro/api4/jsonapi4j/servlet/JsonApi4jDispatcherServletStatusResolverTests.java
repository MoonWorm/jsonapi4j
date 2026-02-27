package pro.api4.jsonapi4j.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonApi4jDispatcherServletStatusResolverTests {

    @Test
    public void strictMode_updateResourceReturns204() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();
        JsonApi4j jsonApi4j = mock(JsonApi4j.class);
        JsonApiRequestSupplier<HttpServletRequest> supplier = mock(JsonApiRequestSupplier.class);
        JsonApiRequest jsonApiRequest = mock(JsonApiRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(supplier.from(request)).thenReturn(jsonApiRequest);
        when(jsonApiRequest.getOperationType()).thenReturn(OperationType.UPDATE_RESOURCE);
        when(jsonApiRequest.getTargetResourceType()).thenReturn(new ResourceType("users"));
        when(jsonApi4j.execute(jsonApiRequest)).thenReturn(null);
        when(jsonApi4j.getCompatibilityMode()).thenReturn(JsonApi4jCompatibilityMode.STRICT);

        setField(sut, "jsonApi4j", jsonApi4j);
        setField(sut, "jsonApiRequestSupplier", supplier);

        sut.service(request, response);

        verify(response).setStatus(204);
    }

    @Test
    public void legacyMode_updateResourceReturns202() throws Exception {
        JsonApi4jDispatcherServlet sut = new JsonApi4jDispatcherServlet();
        JsonApi4j jsonApi4j = mock(JsonApi4j.class);
        JsonApiRequestSupplier<HttpServletRequest> supplier = mock(JsonApiRequestSupplier.class);
        JsonApiRequest jsonApiRequest = mock(JsonApiRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(supplier.from(request)).thenReturn(jsonApiRequest);
        when(jsonApiRequest.getOperationType()).thenReturn(OperationType.UPDATE_RESOURCE);
        when(jsonApiRequest.getTargetResourceType()).thenReturn(new ResourceType("users"));
        when(jsonApi4j.execute(jsonApiRequest)).thenReturn(null);
        when(jsonApi4j.getCompatibilityMode()).thenReturn(JsonApi4jCompatibilityMode.LEGACY);

        setField(sut, "jsonApi4j", jsonApi4j);
        setField(sut, "jsonApiRequestSupplier", supplier);

        sut.service(request, response);

        verify(response).setStatus(202);
    }

    private static void setField(Object target,
                                 String fieldName,
                                 Object value) throws Exception {
        Field field = JsonApi4jDispatcherServlet.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
