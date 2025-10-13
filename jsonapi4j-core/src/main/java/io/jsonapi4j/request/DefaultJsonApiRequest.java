package io.jsonapi4j.request;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ResourceObject;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import io.jsonapi4j.model.document.data.SingleResourceDoc;
import io.jsonapi4j.operation.OperationType;
import io.jsonapi4j.processor.exception.InvalidPayloadException;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Slf4j
@ToString(exclude = {"bodyDeserializer"})
public class DefaultJsonApiRequest implements JsonApiRequest {

    private final BodyDeserializer bodyDeserializer;

    private String resourceId = null;
    private ResourceType targetResourceType;
    private RelationshipName targetRelationshipName;
    private OperationType operationType;

    private Map<String, List<String>> filters = new HashMap<>();

    private Set<String> effectiveIncludes = new HashSet<>();
    private Set<String> originalIncludes = new HashSet<>();

    private String cursor = null;
    private Map<String, List<String>> customQueryParams = new HashMap<>();

    private byte[] payload = new byte[0];

    private Map<String, SortOrder> sortBy = null;

    @Override
    public Map<String, List<String>> getFilters() {
        return filters;
    }

    // lombok workaround
    @Override
    public SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap>> getSingleResourceDocPayload() {
        return JsonApiRequest.super.getSingleResourceDocPayload();
    }

    @Override
    public <A, R> SingleResourceDoc<ResourceObject<A, R>> getSingleResourceDocPayload(Class<A> attType,
                                                                                      Class<R> relType) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            return bodyDeserializer.deserializeResourceDoc(payload, attType, relType);
        } catch (IOException e) {
            log.error("Error deserializing HTTP Request Payload into JsonApiSinglePrimaryResourceDoc. Ensure operation implementation follows JSON:API spec.", e);
            throw new InvalidPayloadException();
        }
    }

    @Override
    public ToOneRelationshipDoc getToOneRelationshipDocPayload() {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            return bodyDeserializer.deserializeRelationshipDoc(payload, ToOneRelationshipDoc.class);
        } catch (IOException e) {
            log.error("Error deserializing HTTP Request Payload into JsonApiSingleDataRelationshipDoc. Ensure operation implementation follows JSON:API spec.", e);
            throw new InvalidPayloadException();
        }
    }

    @Override
    public ToManyRelationshipsDoc getToManyRelationshipDocPayload() {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            return bodyDeserializer.deserializeRelationshipDoc(payload, ToManyRelationshipsDoc.class);
        } catch (IOException e) {
            log.error("Error deserializing HTTP Request Payload into JsonApiSingleDataRelationshipDoc. Ensure operation implementation follows JSON:API spec.", e);
            throw new InvalidPayloadException();
        }
    }

    public interface BodyDeserializer {

        <A, R> SingleResourceDoc<ResourceObject<A, R>> deserializeResourceDoc(
                byte[] payload,
                Class<A> attType,
                Class<R> relType
        ) throws IOException;

        <T> T deserializeRelationshipDoc(byte[] payload, Class<T> type) throws IOException;

    }

}
