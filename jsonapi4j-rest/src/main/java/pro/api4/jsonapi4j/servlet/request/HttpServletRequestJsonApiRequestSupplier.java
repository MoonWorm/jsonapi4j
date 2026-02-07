package pro.api4.jsonapi4j.servlet.request;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.http.exception.NotAcceptableException;
import pro.api4.jsonapi4j.http.exception.UnsupportedMediaTypeException;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Paths;
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
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseSortBy;
import static java.util.stream.Collectors.toMap;

@Data
@Slf4j
public class HttpServletRequestJsonApiRequestSupplier implements JsonApiRequestSupplier<HttpServletRequest> {

    private final ObjectMapper jsonMapper;
    private final OperationDetailsResolver operationDetailsResolver;

    @Override
    public JsonApiRequest from(HttpServletRequest servletRequest) {
        if (!JsonApiMediaType.isAccepted(servletRequest.getHeader(HttpHeaders.ACCEPT.getName()))) {
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
                && !JsonApiMediaType.isMatches(servletRequest.getContentType())) {
            throw new UnsupportedMediaTypeException(servletRequest.getContentType(), JsonApiMediaType.MEDIA_TYPE);
        }

        String path = getPath(servletRequest);
        log.info("Received JSON:API request for the path {} and method {}", path, method);
        log.info("Converting HttpServletRequest to JsonApiRequest...");
        String resourceId = parseResourceIdFromThePath(path);
        Map<String, List<String>> params = getParams(servletRequest);
        Map<String, List<String>> filters = parseFilter(params);
        Set<String> effectiveIncludes = parseEffectiveIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Set<String> originalIncludes = parseOriginalIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Map<String, SortAwareRequest.SortOrder> sortBy = parseSortBy(params.get(SortAwareRequest.SORT_PARAM));
        Map<String, List<String>> customQueryParams = parseCustomQueryParams(params);
        String cursor = parseCursor(params.get(CursorAwareRequest.CURSOR_PARAM));

        OperationDetailsResolver.OperationDetails operationDetails = operationDetailsResolver.fromUrlAndMethod(
                path,
                method
        );
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
        jsonApiRequest.setCursor(cursor);
        jsonApiRequest.setSortBy(sortBy);
        jsonApiRequest.setCustomQueryParams(customQueryParams);
        jsonApiRequest.setPayload(payload);
        log.info("Composed JsonApiRequest: {}", jsonApiRequest);
        return jsonApiRequest;
    }

    private boolean isMethodSupportBody(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method) && !"TRACE".equals(method);
    }

    private String getPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        log.info("Request path: {}", path);
        if(path == null) {
            return "/";
        }
        return Paths.get(path).normalize().toString();
    }

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

}
