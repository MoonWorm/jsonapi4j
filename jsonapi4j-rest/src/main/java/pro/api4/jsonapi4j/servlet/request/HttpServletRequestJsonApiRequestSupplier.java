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
import pro.api4.jsonapi4j.request.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.OperationType.Method.isSupportedMethod;
import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.*;

@Data
@Slf4j
public class HttpServletRequestJsonApiRequestSupplier implements JsonApiRequestSupplier<HttpServletRequest> {

    private final ObjectMapper jsonMapper;
    private final OperationDetailsResolver operationDetailsResolver;

    @Override
    public JsonApiRequest from(HttpServletRequest servletRequest) {
        if (!JsonApiMediaType.isAccepted(servletRequest.getHeaders(HttpHeaders.ACCEPT.getName()))) {
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
        List<String> effectiveIncludes = parseEffectiveIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        List<String> originalIncludes = parseOriginalIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Map<String, SortAwareRequest.SortOrder> sortBy = parseSortBy(params.get(SortAwareRequest.SORT_PARAM));
        Map<String, List<String>> fieldSets = parseFieldSets(params);
        Map<String, List<String>> customQueryParams = parseCustomQueryParams(params);
        String cursor = parseCursor(params.get(CursorAwareRequest.CURSOR_PARAM));
        Long limit = parseLimit(params.get(LimitOffsetAwareRequest.LIMIT_PARAM));
        Long offset = parseOffset(params.get(LimitOffsetAwareRequest.OFFSET_PARAM));
        URI ext = parseExt(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE.getName()));
        URI profile = parseProfile(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE.getName()));

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
        jsonApiRequest.setLimit(limit);
        jsonApiRequest.setOffset(offset);
        jsonApiRequest.setSortBy(sortBy);
        jsonApiRequest.setFieldSets(fieldSets);
        jsonApiRequest.setCustomQueryParams(customQueryParams);
        jsonApiRequest.setPayload(payload);
        jsonApiRequest.setExtension(ext);
        jsonApiRequest.setProfile(profile);
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
