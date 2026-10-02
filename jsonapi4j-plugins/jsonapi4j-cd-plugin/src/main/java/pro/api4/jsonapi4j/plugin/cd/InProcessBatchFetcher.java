package pro.api4.jsonapi4j.plugin.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetch;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetcher;
import pro.api4.jsonapi4j.compound.docs.client.FetchResult;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.Principal;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestBuilder;
import pro.api4.jsonapi4j.servlet.response.ResponseHeaders;
import pro.api4.jsonapi4j.servlet.response.ResponseStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

/**
 * Fetches resources of a type the app serves itself by running the read through {@link JsonApi4j} in-process, rather
 * than calling the app over HTTP. The read goes through the framework as an HTTP one would - validation, plugins such
 * as access control, the operation itself - but no servlet filter sees it.
 *
 * <p>It runs on a worker thread of the resolver, so it carries over what the request thread holds: the request's
 * principal, so access control decides as for the client. Afterwards the thread's principal is restored, and what
 * the read leaves behind - propagated headers, an overridden status - is discarded, as worker threads are pooled.
 * By then the request's own headers and status have long been applied.
 */
@Slf4j
public class InProcessBatchFetcher implements BatchFetcher<DomainSettings.InProcess> {

    private final JsonApi4j jsonApi4j;
    private final ObjectMapper objectMapper;
    private final JsonApiResponseParser responseParser;
    private final List<Propagation> propagation;

    /**
     * @param propagation what of the original request is carried over to the reads - as it would be over HTTP
     */
    public InProcessBatchFetcher(JsonApi4j jsonApi4j, ObjectMapper objectMapper, List<Propagation> propagation) {
        this.jsonApi4j = jsonApi4j;
        this.objectMapper = objectMapper;
        this.responseParser = new JsonApiResponseParser(objectMapper);
        this.propagation = List.copyOf(propagation);
    }

    @Override
    public FetchResult fetch(BatchFetch<DomainSettings.InProcess> chunk, CompoundDocsRequest originalRequest) {
        JsonApiRequest request = toJsonApiRequest(chunk, originalRequest);
        Principal threadPrincipal = AuthenticatedPrincipalContextHolder.copy();
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(originalRequest.getPrincipal());
        try {
            Object document = jsonApi4j.execute(request);
            CacheControlDirectives directives = ResponseHeaders.takeCacheControl();
            return new FetchResult(responseParser.parseResourceObjects(objectMapper.valueToTree(document)), directives);
        } catch (UnsupportedIncludeException e) {
            throw rejectedIncludesOrFailure(chunk, e);
        } catch (RuntimeException e) {
            throw failure(chunk, e);
        } finally {
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(threadPrincipal);
            ResponseHeaders.clear();
            ResponseStatus.clear();
        }
    }

    private JsonApiRequest toJsonApiRequest(BatchFetch<DomainSettings.InProcess> chunk,
                                            CompoundDocsRequest originalRequest) {
        return new JsonApiRequestBuilder()
                .operationType(OperationType.READ_MULTIPLE_RESOURCES)
                .targetResourceType(new ResourceType(chunk.resourceType()))
                .filterBy(Map.of(ID_FILTER_NAME, chunk.ids().stream().sorted().toList()))
                .includes(chunk.includes().stream().sorted().toList())
                .fieldSets(propagation.contains(Propagation.FIELDS) ? originalRequest.getFieldSets() : Map.of())
                .customQueryParams(propagation.contains(Propagation.CUSTOM_QUERY_PARAMS)
                        ? originalRequest.getCustomQueryParams()
                        : Map.of())
                .headers(propagation.contains(Propagation.HEADERS)
                        ? firstValues(originalRequest.getHeaders())
                        : Map.of())
                .sortBy(Map.of())
                .build();
    }

    /**
     * The includes of a chunk name relationships of its resource type directly, so a rejection names them as they
     * were asked for. One rejecting anything else is just a failed read.
     */
    private static RuntimeException rejectedIncludesOrFailure(BatchFetch<DomainSettings.InProcess> chunk,
                                                              UnsupportedIncludeException e) {
        Set<String> rejected = new HashSet<>();
        e.getUnsupportedIncludes().forEach(include -> rejected.add(include.path()));
        rejected.retainAll(chunk.includes());
        return rejected.isEmpty() ? failure(chunk, e) : new RejectedIncludesException(chunk.resourceType(), rejected);
    }

    private static ErrorJsonApiResponseException failure(BatchFetch<DomainSettings.InProcess> chunk,
                                                         RuntimeException e) {
        return new ErrorJsonApiResponseException(String.format(
                "Failed to read '%s' resources %s in-process: %s", chunk.resourceType(), chunk.ids(), e.getMessage()
        ), e);
    }

    private static Map<String, String> firstValues(Map<String, List<String>> headers) {
        Map<String, String> firstValues = new HashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                firstValues.put(name, values.get(0));
            }
        });
        return firstValues;
    }

}
