package pro.api4.jsonapi4j.servlet.request;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.http.exception.NotAcceptableException;
import pro.api4.jsonapi4j.http.exception.UnsupportedMediaTypeException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestBuilder;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.operation.OperationType.Method.isSupportedMethod;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseCursor;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseCustomQueryParams;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseExt;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseFieldSets;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseFilter;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseLimit;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseOffset;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseOriginalIncludes;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseProfile;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseResourceIdFromThePath;
import static pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil.parseSortBy;

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
        log.debug("Received JSON:API request for the path {} and method {}", path, method);
        log.debug("Converting HttpServletRequest to JsonApiRequest...");

        OperationDetailsResolver.OperationDetails operationDetails = operationDetailsResolver.fromUrlAndMethod(
                path,
                method
        );
        String resourceId = parseResourceIdFromThePath(path);
        Map<String, List<String>> params = getParams(servletRequest);
        Map<String, List<String>> filters = parseFilter(params);
        List<String> includes = parseOriginalIncludes(params.get(IncludeAwareRequest.INCLUDE_PARAM));
        Map<String, SortAwareRequest.SortOrder> sortBy = parseSortBy(params.get(SortAwareRequest.SORT_PARAM));
        Map<String, List<String>> fieldSets = parseFieldSets(params);
        Map<String, List<String>> customQueryParams = parseCustomQueryParams(params);
        String cursor = parseCursor(params.get(CursorAwareRequest.CURSOR_PARAM));
        Long limit = parseLimit(params.get(LimitOffsetAwareRequest.LIMIT_PARAM));
        Long offset = parseOffset(params.get(LimitOffsetAwareRequest.OFFSET_PARAM));
        URI ext = parseExt(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE.getName()));
        URI profile = parseProfile(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE.getName()));
        Map<String, String> headers = parseHeaders(servletRequest);

        ResourceType targetResourceType = operationDetails.getResourceType();
        RelationshipName targetRelationshipName = operationDetails.getRelationshipName();
        OperationType targetOperationType = operationDetails.getOperationType();
        JsonApiRequest jsonApiRequest = new JsonApiRequestBuilder()
                .payloadAsBytes(
                        new DefaultJsonApiRequest.BodyDeserializer() {
                            @Override
                            public <A> SingleResourceDoc<ResourceObject<A, LinkedHashMap<String, RelationshipObject>>> deserializeResourceDoc(byte[] payload,
                                                                                                                                              Class<A> attType) throws IOException {
                                TypeFactory typeFactory = jsonMapper.getTypeFactory();
                                JavaType relJavaType = typeFactory.constructMapType(
                                        LinkedHashMap.class,
                                        typeFactory.constructType(String.class),
                                        typeFactory.constructType(RelationshipObject.class)
                                );
                                JavaType jsonApiPrimaryResourceJavaType = typeFactory.constructParametricType(
                                        ResourceObject.class,
                                        typeFactory.constructType(attType),
                                        relJavaType
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

                        },
                        payload
                )
                .resourceId(resourceId)
                .targetResourceType(targetResourceType)
                .targetRelationship(targetRelationshipName)
                .operationType(targetOperationType)
                .filterBy(filters)
                .includes(includes)
                .cursor(cursor)
                .limit(limit)
                .offset(offset)
                .sortBy(sortBy)
                .fieldSets(fieldSets)
                .customQueryParams(customQueryParams)
                .extension(ext)
                .profile(profile)
                .headers(headers)
                .build();
        log.debug("Composed JsonApiRequest: {}", jsonApiRequest);
        return jsonApiRequest;
    }

    private boolean isMethodSupportBody(String method) {
        return "POST".equals(method) || "PATCH".equals(method) || "PUT".equals(method);
    }

    private String getPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        log.debug("Request path: {}", path);
        if (path == null) {
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

    private static Map<String, String> parseHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.put(headerName, headerValue);
            }
        }
        return Collections.unmodifiableMap(headers);
    }

}
