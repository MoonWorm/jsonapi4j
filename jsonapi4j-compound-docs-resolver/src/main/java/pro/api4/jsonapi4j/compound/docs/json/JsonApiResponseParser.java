package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.exception.InvalidJsonApiResponseException;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class JsonApiResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(JsonApiResponseParser.class);

    private final ObjectMapper objectMapper;

    public JsonApiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a primary resource document: the merged relationship linkage of all its primary resources, and each
     * primary resource on its own.
     */
    public ParseResult parsePrimaryResourceDoc(String jsonApiResponse) {
        JsonNode rootNode = readDocument(jsonApiResponse);
        Map<String, Set<IdAndType>> relationships = new HashMap<>();
        List<ParsedResource> primaryResources = new ArrayList<>();
        for (ParsedResource resource : parseResourceObjects(rootNode)) {
            resource.linkage().relationships().forEach((relationshipName, linked) -> relationships
                    .computeIfAbsent(relationshipName, n -> new LinkedHashSet<>())
                    .addAll(linked));
            if (resource.idAndType() != null) {
                primaryResources.add(resource);
            }
        }
        return new ParseResult(
                Collections.unmodifiableMap(relationships),
                Collections.unmodifiableList(primaryResources),
                rootNode
        );
    }

    /**
     * Parses a relationship document, treating its primary data as the linkage of {@code relationshipName}.
     */
    public ParseResult parseRelationshipDoc(String jsonApiResponse, String relationshipName) {
        JsonNode rootNode = readDocument(jsonApiResponse);
        Set<IdAndType> linkage = readLinkage(rootNode.get("data"));
        Map<String, Set<IdAndType>> relationships = linkage.isEmpty()
                ? Collections.emptyMap()
                : Map.of(relationshipName, linkage);
        return new ParseResult(relationships, Collections.emptyList(), rootNode);
    }

    /**
     * Parses a single resource object - e.g. one kept in a cache.
     */
    public ParsedResource parseResource(String jsonApiResource) {
        if (jsonApiResource == null) {
            throw new InvalidJsonApiResponseException("jsonApiResource is null");
        }
        try {
            JsonNode resourceNode = objectMapper.readTree(jsonApiResource);
            return new ParsedResource(
                    new ResourceLinkage(readIdAndType(resourceNode), readRelationships(resourceNode)),
                    jsonApiResource
            );
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse Json:Api resource: {}", jsonApiResource, e);
            throw new InvalidJsonApiResponseException("Failed to parse Json:Api resource: " + jsonApiResource);
        }
    }

    /**
     * @return the include paths an errors document rejects - named in {@code meta.path} of its
     * {@code UNSUPPORTED_INCLUDE} errors - when those are all its errors. None when it holds any other error too, as
     * the request would fail without those paths as well, or is not an errors document at all
     */
    public Set<String> parseUnsupportedIncludes(String errorsDoc) {
        JsonNode rootNode;
        try {
            rootNode = errorsDoc == null ? null : objectMapper.readTree(errorsDoc);
        } catch (JsonProcessingException e) {
            return Collections.emptySet();
        }
        if (rootNode == null || !rootNode.path(ErrorsDoc.ERRORS_FIELD).isArray()) {
            return Collections.emptySet();
        }
        Set<String> paths = new LinkedHashSet<>();
        for (JsonNode error : rootNode.path(ErrorsDoc.ERRORS_FIELD)) {
            JsonNode path = error.path(ErrorObject.META_FIELD).path(UnsupportedIncludeException.PATH_META_FIELD);
            if (!DefaultErrorCodes.UNSUPPORTED_INCLUDE.toCode().equals(error.path(ErrorObject.CODE_FIELD).asText())
                    || !path.isTextual()) {
                return Collections.emptySet();
            }
            paths.add(path.asText());
        }
        return Collections.unmodifiableSet(paths);
    }

    /**
     * @return each resource object of the document's primary data, with its identity and relationship linkage - none
     * for a document without primary data, or one that is not an object at all
     */
    public List<ParsedResource> parseResourceObjects(JsonNode rootNode) {
        if (rootNode == null || !rootNode.isObject()) {
            return Collections.emptyList();
        }
        List<ParsedResource> resources = new ArrayList<>();
        forEachObject(
                rootNode.get(MultipleResourcesDoc.DATA_FIELD),
                node -> resources.add(new ParsedResource(
                        new ResourceLinkage(readIdAndType(node), readRelationships(node)),
                        writeJson(node)
                ))
        );
        return Collections.unmodifiableList(resources);
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonApiResponseException("Failed to serialize JSON:API resource: " + node);
        }
    }

    private JsonNode readDocument(String jsonApiResponse) {
        if (jsonApiResponse == null) {
            throw new InvalidJsonApiResponseException("jsonApiResponse is null");
        }
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonApiResponse);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse Json:Api response: {}", jsonApiResponse, e);
            throw new InvalidJsonApiResponseException("Failed to parse Json:Api response: " + jsonApiResponse);
        }
        if (rootNode == null || !rootNode.isObject()) {
            throw new InvalidJsonApiResponseException("Json:Api response must be a JSON object");
        }
        return rootNode;
    }

    private Map<String, Set<IdAndType>> readRelationships(JsonNode resourceNode) {
        JsonNode relationshipsNode = resourceNode.get(ResourceObject.RELATIONSHIPS_FIELD);
        if (relationshipsNode == null || !relationshipsNode.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, Set<IdAndType>> relationships = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = relationshipsNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> relationship = it.next();
            if (relationship.getValue().isObject()) {
                Set<IdAndType> linkage = readLinkage(relationship.getValue().get(RelationshipObject.DATA_FIELD));
                if (!linkage.isEmpty()) {
                    relationships.put(relationship.getKey(), linkage);
                }
            }
        }
        return Collections.unmodifiableMap(relationships);
    }

    private Set<IdAndType> readLinkage(JsonNode dataNode) {
        Set<IdAndType> linkage = new LinkedHashSet<>();
        forEachObject(dataNode, identifierNode -> {
            IdAndType key = readIdAndType(identifierNode);
            if (key != null) {
                linkage.add(key);
            }
        });
        return linkage;
    }

    /**
     * @return the {@code type} and {@code id} of a resource or resource identifier object, or {@code null} when either
     * is missing or not textual
     */
    public static IdAndType readIdAndType(JsonNode node) {
        String type = readStringValue(node, ResourceIdentifierObject.TYPE_FIELD);
        String id = readStringValue(node, ResourceIdentifierObject.ID_FIELD);
        return type == null || id == null ? null : new IdAndType(id, new ResourceType(type));
    }

    private void forEachObject(JsonNode node, Consumer<JsonNode> consumer) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            consumer.accept(node);
        } else if (node.isArray()) {
            node.forEach(element -> {
                if (element.isObject()) {
                    consumer.accept(element);
                }
            });
        }
    }

    private static String readStringValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && valueNode.isTextual() ? valueNode.asText() : null;
    }

}
