package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.OperationType;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class JsonApiRequestBuilder {

    private DefaultJsonApiRequest.BodyDeserializer bodyDeserializer;
    private byte[] payloadAsBytes;

    private String resourceId;
    private ResourceType targetResourceType;
    private RelationshipName targetRelationshipName;
    private OperationType operationType;
    private Map<String, List<String>> filterBy;
    private List<String> effectiveIncludes;
    private List<String> originalIncludes;
    private Map<String, List<String>> fieldSets;
    private String cursor;
    private Long limit;
    private Long offset;
    private Map<String, SortAwareRequest.SortOrder> sortBy;
    private Map<String, List<String>> customQueryParams;
    private Object payloadAsObject;
    private URI extension;
    private URI profile;

    public JsonApiRequestBuilder() {

    }

    public JsonApiRequestBuilder(JsonApiRequest from) {
        this.resourceId = from.getResourceId();
        this.targetResourceType = from.getTargetResourceType();
        this.targetRelationshipName = from.getTargetRelationshipName();
        this.operationType = from.getOperationType();
        this.filterBy = from.getFilters();
        this.effectiveIncludes = from.getEffectiveIncludes();
        this.originalIncludes = from.getOriginalIncludes();
        this.fieldSets = from.getFieldSets();
        this.cursor = from.getCursor();
        this.limit = from.getLimit();
        this.offset = from.getOffset();
        this.sortBy = from.getSortBy();
        this.customQueryParams = from.getCustomQueryParams();

        this.payloadAsObject = from.getSingleResourceDocPayload();
        if (this.payloadAsObject == null) {
            this.payloadAsObject = from.getToOneRelationshipDocPayload();
        }
        if (this.payloadAsObject == null) {
            this.payloadAsObject = from.getToManyRelationshipDocPayload();
        }

        this.extension = from.getExtension();
        this.profile = from.getProfile();
    }

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

    public JsonApiRequestBuilder customQueryParams(Map<String, List<String>> queryParams) {
        this.customQueryParams = queryParams;
        return this;
    }

    public JsonApiRequestBuilder cursor(String cursor) {
        this.cursor = cursor;
        return this;
    }

    public JsonApiRequestBuilder limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public JsonApiRequestBuilder offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public JsonApiRequestBuilder fieldSets(Map<String, List<String>> fieldSets) {
        this.fieldSets = fieldSets;
        return this;
    }

    public JsonApiRequestBuilder filterBy(Map<String, List<String>> filterBy) {
        this.filterBy = filterBy;
        return this;
    }

    public JsonApiRequestBuilder sortBy(Map<String, SortAwareRequest.SortOrder> sortBy) {
        this.sortBy = sortBy;
        return this;
    }

    public <A, R> JsonApiRequestBuilder payloadObject(SingleResourceDoc<ResourceObject<A, R>> payload) {
        this.payloadAsObject = payload;
        return this;
    }

    public JsonApiRequestBuilder payloadObject(ToOneRelationshipDoc payload) {
        this.payloadAsObject = payload;
        return this;
    }

    public JsonApiRequestBuilder payloadObject(ToManyRelationshipsDoc payload) {
        this.payloadAsObject = payload;
        return this;
    }

    public JsonApiRequestBuilder extension(URI extension) {
        this.extension = extension;
        return this;
    }

    public JsonApiRequestBuilder profile(URI profile) {
        this.profile = profile;
        return this;
    }

    // TODO: merge into .includes
    public JsonApiRequestBuilder originalIncludes(List<String> originalIncludes) {
        this.originalIncludes = originalIncludes;
        return this;
    }

    public JsonApiRequestBuilder effectiveIncludes(List<String> effectiveIncludes) {
        this.effectiveIncludes = effectiveIncludes;
        return this;
    }

    public JsonApiRequestBuilder payloadAsBytes(DefaultJsonApiRequest.BodyDeserializer bodyDeserializer,
                                                byte[] payloadAsBytes) {
        this.bodyDeserializer = bodyDeserializer;
        this.payloadAsBytes = payloadAsBytes;
        return this;
    }

    public JsonApiRequest build() {
        DefaultJsonApiRequest request;
        if (this.bodyDeserializer != null) {
            request = new DefaultJsonApiRequest(this.bodyDeserializer);
            request.setPayload(payloadAsBytes);
        } else {
            request = new DefaultJsonApiRequest(new DefaultJsonApiRequest.BodyDeserializer() {
                @Override
                public <A, R> SingleResourceDoc<ResourceObject<A, R>> deserializeResourceDoc(byte[] p,
                                                                                             Class<A> attType,
                                                                                             Class<R> relType) throws IOException {
                    //noinspection unchecked
                    return (SingleResourceDoc<ResourceObject<A, R>>) payloadAsObject;
                }

                @Override
                public <T> T deserializeRelationshipDoc(byte[] p,
                                                        Class<T> type) throws IOException {
                    //noinspection unchecked
                    return (T) payloadAsObject;
                }
            });
            request.setPayload(new byte[]{1}); // to bypass not null/not empty validation
        }

        request.setResourceId(resourceId);
        request.setTargetResourceType(targetResourceType);
        request.setTargetRelationshipName(targetRelationshipName);
        request.setOperationType(operationType);
        request.setFilters(filterBy);
        request.setEffectiveIncludes(effectiveIncludes);
        request.setOriginalIncludes(originalIncludes);
        request.setCursor(cursor);
        request.setLimit(limit);
        request.setOffset(offset);
        request.setCustomQueryParams(customQueryParams);
        request.setSortBy(sortBy);
        request.setFieldSets(fieldSets);
        request.setExtension(extension);
        request.setProfile(profile);
        return request;
    }

}
