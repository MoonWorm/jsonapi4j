package io.jsonapi4j.request;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.model.document.data.ResourceObject;
import io.jsonapi4j.model.document.data.SingleResourceDoc;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import io.jsonapi4j.operation.OperationType;
import io.jsonapi4j.request.util.JsonApiRequestParsingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonApiRequestBuilder {

    private String resourceId = null;
    private ResourceType targetResourceType;
    private RelationshipName targetRelationshipName;
    private OperationType operationType;
    private Map<String, List<String>> filterBy = new HashMap<>();
    private Set<String> effectiveIncludes = new HashSet<>();
    private Set<String> originalIncludes = new HashSet<>();
    private String cursor = null;
    private Map<String, SortAwareRequest.SortOrder> sortBy = new HashMap<>();
    private Map<String, List<String>> queryParams = new HashMap<>();
    private Object payload;

    public JsonApiRequestBuilder resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public JsonApiRequestBuilder targetResourceType(ResourceType targetResourceType) {
        this.targetResourceType = targetResourceType;
        return this;
    }

    public JsonApiRequestBuilder targetRelationship(RelationshipName targetRelationshipName) {
        this.targetRelationshipName = targetRelationshipName;
        return this;
    }

    public JsonApiRequestBuilder operationType(OperationType operationType) {
        this.operationType = operationType;
        return this;
    }

    public JsonApiRequestBuilder queryParams(Map<String, List<String>> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public JsonApiRequestBuilder filterBy(String filterName,
                                          List<String> filterValue) {
        this.filterBy.put(filterName, filterValue);
        return this;
    }

    public JsonApiRequestBuilder addSortBy(String sortBy,
                                           SortAwareRequest.SortOrder sortOrder) {
        this.sortBy.put(sortBy, sortOrder);
        return this;
    }

    public <A, R> JsonApiRequestBuilder payload(SingleResourceDoc<ResourceObject<A, R>> payload) {
        this.payload = payload;
        return this;
    }

    public JsonApiRequestBuilder payload(ToOneRelationshipDoc payload) {
        this.payload = payload;
        return this;
    }

    public JsonApiRequestBuilder payload(ToManyRelationshipsDoc payload) {
        this.payload = payload;
        return this;
    }

    public JsonApiRequestBuilder includes(Set<String> includes) {
        this.originalIncludes.addAll(JsonApiRequestParsingUtil.parseOriginalIncludes(List.copyOf(includes)));
        this.effectiveIncludes.addAll(JsonApiRequestParsingUtil.parseEffectiveIncludes(List.copyOf(includes)));
        return this;
    }

    public JsonApiRequest build() {
        DefaultJsonApiRequest request = new DefaultJsonApiRequest(new DefaultJsonApiRequest.BodyDeserializer() {
            @Override
            public <A, R> SingleResourceDoc<ResourceObject<A, R>> deserializeResourceDoc(byte[] p,
                                                                                         Class<A> attType,
                                                                                         Class<R> relType) throws IOException {
                //noinspection unchecked
                return (SingleResourceDoc<ResourceObject<A, R>>) payload;
            }

            @Override
            public <T> T deserializeRelationshipDoc(byte[] p,
                                                    Class<T> type) throws IOException {
                //noinspection unchecked
                return (T) payload;
            }
        });
        request.setPayload(new byte[]{1}); // to bypass not null/not empty validation
        request.setResourceId(resourceId);
        request.setTargetResourceType(targetResourceType);
        request.setTargetRelationshipName(targetRelationshipName);
        request.setOperationType(operationType);
        request.setFilters(filterBy);
        request.setEffectiveIncludes(effectiveIncludes);
        request.setCursor(cursor);
        request.setSortBy(sortBy);
        request.setCustomQueryParams(queryParams);
        return request;
    }

}
