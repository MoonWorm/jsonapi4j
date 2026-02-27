package pro.api4.jsonapi4j.servlet.request;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.http.exception.NotAcceptableException;
import pro.api4.jsonapi4j.http.exception.UnsupportedMediaTypeException;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ConflictJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ForbiddenJsonApiRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.OperationType.Method.isSupportedMethod;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseCursor;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseCustomQueryParams;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseEffectiveIncludes;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseFilter;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseOriginalIncludes;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseResourceIdFromThePath;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseSparseFieldsets;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseSortBy;
import static java.util.stream.Collectors.toMap;

@Data
@Slf4j
public class HttpServletRequestJsonApiRequestSupplier implements JsonApiRequestSupplier<HttpServletRequest> {

    private final ObjectMapper jsonMapper;
    private final OperationDetailsResolver operationDetailsResolver;
    private final JsonApi4jCompatibilityMode compatibilityMode;
    private final Set<String> supportedExtensions;
    private final Set<String> supportedProfiles;

    public HttpServletRequestJsonApiRequestSupplier(ObjectMapper jsonMapper,
                                                    OperationDetailsResolver operationDetailsResolver) {
        this(jsonMapper, operationDetailsResolver, JsonApi4jCompatibilityMode.STRICT);
    }

    public HttpServletRequestJsonApiRequestSupplier(ObjectMapper jsonMapper,
                                                    OperationDetailsResolver operationDetailsResolver,
                                                    JsonApi4jCompatibilityMode compatibilityMode) {
        this(jsonMapper, operationDetailsResolver, compatibilityMode, Collections.emptySet(), Collections.emptySet());
    }

    public HttpServletRequestJsonApiRequestSupplier(ObjectMapper jsonMapper,
                                                    OperationDetailsResolver operationDetailsResolver,
                                                    JsonApi4jCompatibilityMode compatibilityMode,
                                                    Set<String> supportedExtensions,
                                                    Set<String> supportedProfiles) {
        this.jsonMapper = jsonMapper;
        this.operationDetailsResolver = operationDetailsResolver;
        this.compatibilityMode = compatibilityMode == null
                ? JsonApi4jCompatibilityMode.STRICT
                : compatibilityMode;
        this.supportedExtensions = normalizeSupportedUris(supportedExtensions);
        this.supportedProfiles = normalizeSupportedUris(supportedProfiles);
    }

    @Override
    public JsonApiRequest from(HttpServletRequest servletRequest) {
        if (!JsonApiMediaType.isAccepted(
                servletRequest.getHeader(HttpHeaders.ACCEPT.getName()),
                compatibilityMode,
                supportedExtensions,
                supportedProfiles
        )) {
            throw new NotAcceptableException(servletRequest.getHeader(HttpHeaders.ACCEPT.getName()), JsonApiMediaType.MEDIA_TYPE);
        }

        if (!isSupportedMethod(servletRequest.getMethod())) {
            throw new MethodNotSupportedException(
                    servletRequest.getMethod(),
                    Arrays.stream(OperationType.Method.values()).map(Enum::name).collect(Collectors.joining(", "))
            );
        }

        String method = servletRequest.getMethod();

        byte[] payload;
        try {
            payload = servletRequest.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Error while reading request body", e);
            throw new RuntimeException(e);
        }

        if (isMethodSupportBody(method)
                && payload.length > 0
                && !JsonApiMediaType.isMatches(
                        servletRequest.getContentType(),
                        compatibilityMode,
                        supportedExtensions,
                        supportedProfiles
                )) {
            throw new UnsupportedMediaTypeException(servletRequest.getContentType(), JsonApiMediaType.MEDIA_TYPE);
        }

        String path = getPath(servletRequest);
        log.info(
                "Received JSON:API request. requestId={}, method={}, path={}",
                resolveRequestId(servletRequest),
                method,
                path
        );
        String resourceId = parseResourceIdFromThePath(path);
        Map<String, List<String>> params = getParams(servletRequest);
        Map<String, List<String>> filters = parseFilter(params);
        Set<String> effectiveIncludes = parseEffectiveIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Set<String> originalIncludes = parseOriginalIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Map<String, Set<String>> sparseFieldsets = parseSparseFieldsets(params);
        Map<String, SortAwareRequest.SortOrder> sortBy = parseSortBy(params.get(SortAwareRequest.SORT_PARAM));
        Map<String, List<String>> customQueryParams = parseCustomQueryParams(params);
        String cursor = parseCursor(params.get(CursorAwareRequest.CURSOR_PARAM));

        OperationDetailsResolver.OperationDetails operationDetails = operationDetailsResolver.fromUrlAndMethod(
                path,
                method
        );
        operationDetailsResolver.validateIncludes(originalIncludes, operationDetails, compatibilityMode);
        ResourceType targetResourceType = operationDetails.getResourceType();
        RelationshipName targetRelationshipName = operationDetails.getRelationshipName();
        OperationType targetOperationType = operationDetails.getOperationType();
        DefaultJsonApiRequest jsonApiRequest = new DefaultJsonApiRequest(
                new DefaultJsonApiRequest.BodyDeserializer() {
                    @Override
                    public <A, R> SingleResourceDoc<ResourceObject<A, R>> deserializeResourceDoc(byte[] payload,
                                                                                                 Class<A> attType,
                                                                                                 Class<R> relType) throws IOException {
                        TypeFactory typeFactory = jsonMapper.getTypeFactory();
                        JavaType jsonApiPrimaryResourceJavaType = typeFactory.constructParametricType(
                                ResourceObject.class,
                                attType,
                                relType
                        );
                        JavaType jsonApiSinglePrimaryResourceDocJavaType = typeFactory.constructParametricType(
                                SingleResourceDoc.class,
                                jsonApiPrimaryResourceJavaType
                        );
                        return jsonMapper.readValue(payload, jsonApiSinglePrimaryResourceDocJavaType);
                    }

                    @Override
                    public <T> T deserializeRelationshipDoc(byte[] payload,
                                                            Class<T> type) throws IOException {
                        return jsonMapper.readValue(payload, type);
                    }

                }
        );
        jsonApiRequest.setResourceId(resourceId);
        jsonApiRequest.setTargetResourceType(targetResourceType);
        jsonApiRequest.setTargetRelationshipName(targetRelationshipName);
        jsonApiRequest.setOperationType(targetOperationType);
        jsonApiRequest.setFilters(filters);
        jsonApiRequest.setEffectiveIncludes(effectiveIncludes);
        jsonApiRequest.setOriginalIncludes(originalIncludes);
        jsonApiRequest.setSparseFieldsets(sparseFieldsets);
        jsonApiRequest.setCursor(cursor);
        jsonApiRequest.setSortBy(sortBy);
        jsonApiRequest.setCustomQueryParams(customQueryParams);
        jsonApiRequest.setPayload(payload);
        validateStrictJsonApiRequest(jsonApiRequest);
        if (log.isDebugEnabled()) {
            log.debug(
                    "Composed JsonApiRequest metadata: operation={}, resourceType={}, resourceId={}, relationship={}, filters={}, includeCount={}, sparseFieldsetTypes={}, sortFields={}, customQueryParams={}",
                    targetOperationType,
                    targetResourceType == null ? null : targetResourceType.getType(),
                    resourceId,
                    targetRelationshipName == null ? null : targetRelationshipName.getName(),
                    filters.keySet(),
                    originalIncludes.size(),
                    sparseFieldsets.keySet(),
                    sortBy.keySet(),
                    customQueryParams.keySet()
            );
        }
        return jsonApiRequest;
    }

    private void validateStrictJsonApiRequest(DefaultJsonApiRequest request) {
        if (compatibilityMode != JsonApi4jCompatibilityMode.STRICT) {
            return;
        }
        switch (request.getOperationType()) {
            case CREATE_RESOURCE -> validateCreateResourceRequest(request);
            case UPDATE_RESOURCE -> validateUpdateResourceRequest(request);
            case UPDATE_TO_ONE_RELATIONSHIP -> validateUpdateToOneRelationshipRequest(request);
            case UPDATE_TO_MANY_RELATIONSHIP, ADD_TO_MANY_RELATIONSHIP, REMOVE_FROM_MANY_RELATIONSHIP ->
                    validateToManyRelationshipRequest(request);
        }
    }

    private void validateCreateResourceRequest(DefaultJsonApiRequest request) {
        JsonNode payloadData = requireDataObject(request);
        String payloadType = getTextOrNull(payloadData.get("type"));
        String expectedType = request.getTargetResourceType().getType();

        if (StringUtils.isBlank(payloadType)) {
            throw invalidPayload("requestBody.data.type", "Resource type is required in requestBody.data.type");
        }
        if (!expectedType.equals(payloadType)) {
            throw new ConflictJsonApiRequestException(
                    "requestBody.data.type",
                    "Payload type '%s' doesn't match target resource type '%s'".formatted(payloadType, expectedType)
            );
        }
    }

    private void validateUpdateResourceRequest(DefaultJsonApiRequest request) {
        JsonNode payloadData = requireDataObject(request);
        String payloadType = getTextOrNull(payloadData.get("type"));
        String expectedType = request.getTargetResourceType().getType();
        String payloadId = getTextOrNull(payloadData.get("id"));
        String expectedId = request.getResourceId();

        if (StringUtils.isBlank(payloadType)) {
            throw invalidPayload("requestBody.data.type", "Resource type is required in requestBody.data.type");
        }
        if (!expectedType.equals(payloadType)) {
            throw new ConflictJsonApiRequestException(
                    "requestBody.data.type",
                    "Payload type '%s' doesn't match target resource type '%s'".formatted(payloadType, expectedType)
            );
        }
        if (StringUtils.isBlank(payloadId)) {
            throw invalidPayload("requestBody.data.id", "Resource id is required in requestBody.data.id");
        }
        if (!expectedId.equals(payloadId)) {
            throw new ConflictJsonApiRequestException(
                    "requestBody.data.id",
                    "Payload id '%s' doesn't match target resource id '%s'".formatted(payloadId, expectedId)
            );
        }

        JsonNode relationships = payloadData.get("relationships");
        boolean relationshipModificationRequested = relationships != null
                && !relationships.isNull()
                && (!relationships.isObject() || relationships.size() > 0);
        if (relationshipModificationRequested) {
            throw new ForbiddenJsonApiRequestException(
                    "requestBody.data.relationships",
                    "Resource-level relationship replacement is not supported. Use /{resourceType}/{id}/relationships/{relationshipName} endpoints."
            );
        }
    }

    private void validateUpdateToOneRelationshipRequest(DefaultJsonApiRequest request) {
        JsonNode payloadRoot = parsePayloadRoot(request);
        if (!payloadRoot.has("data")) {
            throw invalidPayload(
                    "requestBody.data",
                    "To-one relationship update must contain requestBody.data (resource identifier or null)"
            );
        }
        JsonNode data = payloadRoot.get("data");
        if (data == null || data.isNull()) {
            return;
        }
        if (!data.isObject()) {
            throw invalidPayload("requestBody.data", "Resource identifier must be a JSON object or null");
        }
        validateResourceIdentifierNode(data, "requestBody.data");
    }

    private void validateToManyRelationshipRequest(DefaultJsonApiRequest request) {
        JsonNode payloadRoot = parsePayloadRoot(request);
        JsonNode data = payloadRoot.get("data");
        if (data == null || !data.isArray()) {
            throw invalidPayload(
                    "requestBody.data",
                    "To-many relationship request must contain requestBody.data array of resource identifiers"
            );
        }
        for (int i = 0; i < data.size(); i++) {
            JsonNode resourceIdentifier = data.get(i);
            if (resourceIdentifier == null || !resourceIdentifier.isObject()) {
                throw invalidPayload(
                        "requestBody.data[%d]".formatted(i),
                        "Resource identifier must be a JSON object"
                );
            }
            validateResourceIdentifierNode(resourceIdentifier, "requestBody.data[%d]".formatted(i));
        }
    }

    private void validateResourceIdentifierNode(JsonNode resourceIdentifier,
                                                String parameterPrefix) {
        if (StringUtils.isBlank(getTextOrNull(resourceIdentifier.get("type")))) {
            throw invalidPayload(parameterPrefix + ".type", "Resource identifier type must not be blank");
        }
        String id = getTextOrNull(resourceIdentifier.get("id"));
        String lid = getTextOrNull(resourceIdentifier.get("lid"));
        if (StringUtils.isBlank(id) && StringUtils.isBlank(lid)) {
            throw invalidPayload(
                    parameterPrefix + ".id",
                    "Resource identifier must include non-blank id or lid"
            );
        }
    }

    private JsonNode requireDataObject(DefaultJsonApiRequest request) {
        JsonNode payloadRoot = parsePayloadRoot(request);
        JsonNode data = payloadRoot.get("data");
        if (data == null || !data.isObject()) {
            throw invalidPayload(
                    "requestBody.data",
                    "Request must contain requestBody.data as a single resource object"
            );
        }
        return data;
    }

    private JsonNode parsePayloadRoot(DefaultJsonApiRequest request) {
        byte[] payload = request.getPayload();
        if (payload == null || payload.length == 0) {
            throw invalidPayload("requestBody", "Request body is required");
        }
        try {
            return jsonMapper.readTree(payload);
        } catch (IOException e) {
            throw invalidPayload(
                    "requestBody",
                    "Couldn't deserialize payload into a valid JSON:API document. Refer JSON:API spec for more details."
            );
        }
    }

    private String getTextOrNull(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        return jsonNode.asText(null);
    }

    private BadJsonApiRequestException invalidPayload(String parameter,
                                                      String detail) {
        return new BadJsonApiRequestException(DefaultErrorCodes.INVALID_PAYLOAD, parameter, detail);
    }

    private boolean isMethodSupportBody(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method) && !"TRACE".equals(method);
    }

    private Set<String> normalizeSupportedUris(Set<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String uri : uris) {
            if (uri != null && !uri.isBlank()) {
                normalized.add(uri.trim());
            }
        }
        return normalized;
    }

    private String getPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if(path == null) {
            return "/";
        }
        return Paths.get(path).normalize().toString();
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return "n/a";
        }
        String requestId = request.getHeader("X-Request-Id");
        if (StringUtils.isBlank(requestId)) {
            requestId = request.getHeader("X-Correlation-Id");
        }
        return StringUtils.isBlank(requestId) ? "n/a" : requestId;
    }

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

}
