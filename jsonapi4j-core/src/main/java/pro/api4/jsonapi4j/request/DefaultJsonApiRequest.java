package pro.api4.jsonapi4j.request;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.exception.InvalidPayloadException;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Slf4j
@ToString(exclude = {"bodyDeserializer"})
public class DefaultJsonApiRequest implements JsonApiRequest {

    private final BodyDeserializer bodyDeserializer;

    private String resourceId = null;
    private ResourceType targetResourceType;
    private RelationshipName targetRelationshipName;
    private OperationType operationType;

    private Map<String, List<String>> filters = new LinkedHashMap<>();

    private List<String> effectiveIncludes = new ArrayList<>();
    private List<String> originalIncludes = new ArrayList<>();

    private String cursor = null;

    private Long limit = null;
    private Long offset = null;

    private Map<String, List<String>> customQueryParams = new LinkedHashMap<>();

    private byte[] payload = new byte[0];
    private final Map<String, Object> payloadCache = new HashMap<>();

    private Map<String, SortOrder> sortBy = new LinkedHashMap<>();

    private Map<String, List<String>> fieldSets = new LinkedHashMap<>();

    private URI extension;
    private URI profile;

    private Map<String, String> headers = new LinkedHashMap<>();

    // lombok workaround
    @Override
    public SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap<String, RelationshipObject>>> getSingleResourceDocPayload() {
        return JsonApiRequest.super.getSingleResourceDocPayload();
    }

    @Override
    public <A> SingleResourceDoc<ResourceObject<A, LinkedHashMap<String, RelationshipObject>>> getSingleResourceDocPayload(Class<A> attType) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        String key = cacheKey(attType);
        if (payloadCache.containsKey(key)) {
            //noinspection unchecked
            return (SingleResourceDoc<ResourceObject<A, LinkedHashMap<String, RelationshipObject>>>) payloadCache.get(key);
        }
        try {
            SingleResourceDoc<ResourceObject<A, LinkedHashMap<String, RelationshipObject>>> doc = bodyDeserializer.deserializeResourceDoc(payload, attType);
            payloadCache.put(key, doc);
            return doc;
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
        String key = cacheKey(ToOneRelationshipDoc.class);
        if (payloadCache.containsKey(key)) {
            return (ToOneRelationshipDoc) payloadCache.get(key);
        }
        try {
            ToOneRelationshipDoc doc = bodyDeserializer.deserializeRelationshipDoc(payload, ToOneRelationshipDoc.class);
            payloadCache.put(key, doc);
            return doc;
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
        String key = cacheKey(ToManyRelationshipsDoc.class);
        if (payloadCache.containsKey(key)) {
            return (ToManyRelationshipsDoc) payloadCache.get(key);
        }
        try {
            ToManyRelationshipsDoc doc = bodyDeserializer.deserializeRelationshipDoc(payload, ToManyRelationshipsDoc.class);
            payloadCache.put(key, doc);
            return doc;
        } catch (IOException e) {
            log.error("Error deserializing HTTP Request Payload into JsonApiSingleDataRelationshipDoc. Ensure operation implementation follows JSON:API spec.", e);
            throw new InvalidPayloadException();
        }
    }

    public interface BodyDeserializer {

        <A> SingleResourceDoc<ResourceObject<A, LinkedHashMap<String, RelationshipObject>>> deserializeResourceDoc(
                byte[] payload,
                Class<A> attType
        ) throws IOException;

        <T> T deserializeRelationshipDoc(byte[] payload, Class<T> type) throws IOException;

    }

    private static String cacheKey(Class<?> attType) {
        return attType.getName();
    }

    public static JsonApiRequestBuilder builder() {
        return new JsonApiRequestBuilder();
    }

}
