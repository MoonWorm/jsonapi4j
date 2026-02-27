package pro.api4.jsonapi4j.servlet.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ConflictJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ForbiddenJsonApiRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpServletRequestJsonApiRequestSupplierValidationTests {

    @Test
    public void strictMode_createTypeMismatch_returnsConflictException() throws IOException {
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

        HttpServletRequest request = mockRequest(
                "POST",
                "/users",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "posts",
                            "attributes": {
                              "name": "John"
                            }
                          }
                        }
                        """
        );

        assertThatThrownBy(() -> sut.from(request))
                .isInstanceOf(ConflictJsonApiRequestException.class)
                .hasMessageContaining("Payload type");
    }

    @Test
    public void strictMode_updateIdMismatch_returnsConflictException() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123", "PATCH")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.UPDATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "PATCH",
                "/users/123",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "users",
                            "id": "999",
                            "attributes": {
                              "name": "John"
                            }
                          }
                        }
                        """
        );

        assertThatThrownBy(() -> sut.from(request))
                .isInstanceOf(ConflictJsonApiRequestException.class)
                .hasMessageContaining("Payload id");
    }

    @Test
    public void strictMode_updateWithRelationships_returnsForbiddenException() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123", "PATCH")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.UPDATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "PATCH",
                "/users/123",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "users",
                            "id": "123",
                            "relationships": {
                              "citizenships": {
                                "data": [
                                  {
                                    "type": "countries",
                                    "id": "NO"
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """
        );

        assertThatThrownBy(() -> sut.from(request))
                .isInstanceOf(ForbiddenJsonApiRequestException.class)
                .hasMessageContaining("relationship replacement is not supported");
    }

    @Test
    public void strictMode_toManyAddRequiresArrayLinkagePayload_returnsBadRequestException() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123/relationships/citizenships", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.ADD_TO_MANY_RELATIONSHIP,
                        new ResourceType("users"),
                        new RelationshipName("citizenships")
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "POST",
                "/users/123/relationships/citizenships",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "countries",
                            "id": "NO"
                          }
                        }
                        """
        );

        assertThatThrownBy(() -> sut.from(request))
                .isInstanceOf(BadJsonApiRequestException.class)
                .hasMessageContaining("requestBody.data array");
    }

    @Test
    public void legacyMode_updateWithRelationships_isAllowed() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123", "PATCH")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.UPDATE_RESOURCE,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.LEGACY
        );

        HttpServletRequest request = mockRequest(
                "PATCH",
                "/users/123",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "users",
                            "id": "123",
                            "relationships": {
                              "citizenships": {
                                "data": []
                              }
                            }
                          }
                        }
                        """
        );

        JsonApiRequest jsonApiRequest = sut.from(request);
        assertThat(jsonApiRequest).isNotNull();
        assertThat(jsonApiRequest.getOperationType()).isEqualTo(OperationType.UPDATE_RESOURCE);
    }

    @Test
    public void getRequest_sparseFieldsets_areParsedIntoRequestModel() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/1", "GET")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.READ_RESOURCE_BY_ID,
                        new ResourceType("users"),
                        null
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "GET",
                "/users/1",
                "application/vnd.api+json",
                "",
                Map.of(
                        "fields[users]", new String[]{"fullName,email"},
                        "fields[countries]", new String[]{"name"}
                )
        );

        JsonApiRequest jsonApiRequest = sut.from(request);

        assertThat(jsonApiRequest.getSparseFieldsets())
                .containsEntry("users", Set.of("fullName", "email"))
                .containsEntry("countries", Set.of("name"));
        assertThat(jsonApiRequest.getCustomQueryParams()).isEmpty();
    }

    @Test
    public void strictMode_toOneRelationship_withLidAndNoId_isAllowed() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123/relationships/manager", "PATCH")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.UPDATE_TO_ONE_RELATIONSHIP,
                        new ResourceType("users"),
                        new RelationshipName("manager")
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "PATCH",
                "/users/123/relationships/manager",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "users",
                            "lid": "tmp-manager-1"
                          }
                        }
                        """
        );

        JsonApiRequest jsonApiRequest = sut.from(request);
        assertThat(jsonApiRequest).isNotNull();
    }

    @Test
    public void strictMode_toManyRelationship_withLidAndNoId_isAllowedAndDeserialized() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123/relationships/relatives", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.ADD_TO_MANY_RELATIONSHIP,
                        new ResourceType("users"),
                        new RelationshipName("relatives")
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "POST",
                "/users/123/relationships/relatives",
                "application/vnd.api+json",
                """
                        {
                          "data": [
                            {
                              "type": "users",
                              "lid": "tmp-relative-1"
                            }
                          ]
                        }
                        """
        );

        JsonApiRequest jsonApiRequest = sut.from(request);
        ToManyRelationshipsDoc relationshipDoc = jsonApiRequest.getToManyRelationshipDocPayload();
        assertThat(relationshipDoc.getData()).hasSize(1);
        ResourceIdentifierObject identifierObject = relationshipDoc.getData().getFirst();
        assertThat(identifierObject.getId()).isNull();
        assertThat(identifierObject.getLid()).isEqualTo("tmp-relative-1");
        assertThat(identifierObject.getType()).isEqualTo("users");
    }

    @Test
    public void strictMode_toManyRelationship_missingIdAndLid_returnsBadRequest() throws IOException {
        OperationDetailsResolver operationDetailsResolver = mock(OperationDetailsResolver.class);
        when(operationDetailsResolver.fromUrlAndMethod("/users/123/relationships/relatives", "POST")).thenReturn(
                new OperationDetailsResolver.OperationDetails(
                        OperationType.ADD_TO_MANY_RELATIONSHIP,
                        new ResourceType("users"),
                        new RelationshipName("relatives")
                )
        );

        HttpServletRequestJsonApiRequestSupplier sut = new HttpServletRequestJsonApiRequestSupplier(
                new ObjectMapper(),
                operationDetailsResolver,
                JsonApi4jCompatibilityMode.STRICT
        );

        HttpServletRequest request = mockRequest(
                "POST",
                "/users/123/relationships/relatives",
                "application/vnd.api+json",
                """
                        {
                          "data": [
                            {
                              "type": "users"
                            }
                          ]
                        }
                        """
        );

        assertThatThrownBy(() -> sut.from(request))
                .isInstanceOf(BadJsonApiRequestException.class)
                .hasMessageContaining("id or lid");
    }

    @Test
    public void strictMode_createResource_withLidAndNoId_isDeserialized() throws IOException {
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

        HttpServletRequest request = mockRequest(
                "POST",
                "/users",
                "application/vnd.api+json",
                """
                        {
                          "data": {
                            "type": "users",
                            "lid": "tmp-user-1",
                            "attributes": {
                              "fullName": "John Doe"
                            }
                          }
                        }
                        """
        );

        JsonApiRequest jsonApiRequest = sut.from(request);
        SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap>> payload =
                jsonApiRequest.getSingleResourceDocPayload(LinkedHashMap.class, LinkedHashMap.class);

        assertThat(payload).isNotNull();
        assertThat(payload.getData().getId()).isNull();
        assertThat(payload.getData().getLid()).isEqualTo("tmp-user-1");
        assertThat(payload.getData().getType()).isEqualTo("users");
    }

    private static HttpServletRequest mockRequest(String method,
                                                  String pathInfo,
                                                  String contentType,
                                                  String payload) throws IOException {
        return mockRequest(method, pathInfo, contentType, payload, Map.of());
    }

    private static HttpServletRequest mockRequest(String method,
                                                  String pathInfo,
                                                  String contentType,
                                                  String payload,
                                                  Map<String, String[]> params) throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT.getName())).thenReturn("application/vnd.api+json");
        when(request.getMethod()).thenReturn(method);
        when(request.getInputStream()).thenReturn(inputStream(payload));
        when(request.getContentType()).thenReturn(contentType);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getParameterMap()).thenReturn(params);
        return request;
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
