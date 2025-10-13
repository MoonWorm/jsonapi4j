package io.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonapi4j.compound.docs.exception.InvalidJsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonApiResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(JsonApiResponseParser.class);

    private final ObjectMapper objectMapper;

    public JsonApiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParseResult parsePrimaryResourceDoc(String jsonApiResponse) {
        return parseDoc(jsonApiResponse, this::parseResourceDocData);
    }

    public ParseResult parseRelationshipDoc(String jsonApiResponse, String relationshipName) {
        return parseDoc(jsonApiResponse, dataNode -> parseRelationshipDocData(dataNode, relationshipName));
    }

    private ParseResult parseDoc(String jsonApiResponse, Function<JsonNode, IntermediateParseResult> dataProcessor) {
        if (jsonApiResponse == null) {
            throw new InvalidJsonApiResponse("jsonApiResponse is null");
        }
        try {
            JsonNode rootNode = objectMapper.readTree(jsonApiResponse);
            IntermediateParseResult parseResult = parseData(rootNode, dataProcessor);
            return new ParseResult(parseResult.typeToIdsMap, parseResult.typeToRelationshipNamesMap, rootNode);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse Json:Api response: " + jsonApiResponse, e);
            throw new InvalidJsonApiResponse("Failed to parse Json:Api response: " + jsonApiResponse);
        }
    }

    public IntermediateParseResult parseResourceDocData(String jsonApiResource) {
        if (jsonApiResource == null) {
            throw new InvalidJsonApiResponse("jsonApiResource is null");
        }
        try {
            JsonNode rootNode = objectMapper.readTree(jsonApiResource);
            return parseResourceDocData(rootNode);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse Json:Api resource: " + jsonApiResource, e);
            throw new InvalidJsonApiResponse("Failed to parse Json:Api resource: " + jsonApiResource);
        }
    }

    private IntermediateParseResult parseResourceDocData(JsonNode dataNode) {
        Map<String, Set<String>> typeToIdsMap = new HashMap<>();
        Map<String, Set<String>> typeToRelationshipNamesMap = new HashMap<>();
        processObjectOrArray(dataNode,
                n -> {
                    IntermediateParseResult intermediateParseResult = processRelationshipsNode(n);
                    intermediateParseResult.typeToIdsMap().forEach((key, value) -> typeToIdsMap.merge(
                            key,
                            value,
                            (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet())
                    ));
                    intermediateParseResult.typeToRelationshipNamesMap().forEach((key, value) -> typeToRelationshipNamesMap.merge(
                            key,
                            value,
                            (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet())
                    ));
                }
        );
        return new IntermediateParseResult(
                Collections.unmodifiableMap(typeToIdsMap),
                Collections.unmodifiableMap(typeToRelationshipNamesMap)
        );
    }

    private IntermediateParseResult parseRelationshipDocData(JsonNode dataNode, String relationshipName) {
        Map<String, Set<String>> typeToIdsMap = new HashMap<>();
        Map<String, Set<String>> typeToRelationshipNamesMap = new HashMap<>();
        processObjectOrArray(dataNode,
                n -> {
                    IntermediateParseResult intermediateParseResult = processDataNodeForRelationshipDoc(n, relationshipName);
                    intermediateParseResult.typeToIdsMap().forEach((key, value) -> typeToIdsMap.merge(
                            key,
                            value,
                            (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet())
                    ));
                    intermediateParseResult.typeToRelationshipNamesMap().forEach((key, value) -> typeToRelationshipNamesMap.merge(
                            key,
                            value,
                            (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet())
                    ));
                }
        );
        return new IntermediateParseResult(
                Collections.unmodifiableMap(typeToIdsMap),
                Collections.unmodifiableMap(typeToRelationshipNamesMap)
        );
    }

    private IntermediateParseResult processDataNodeForRelationshipDoc(JsonNode dataObjectNode, String relationshipName) {
        String type = dataObjectNode.get("type").asText();
        String id = dataObjectNode.get("id").asText();
        LOG.debug("Processing data object for relationship doc: type = {}, id = {}", type, id);
        Map<String, Set<String>> typeToIdsMap = Map.of(type, Set.of(id));
        Map<String, Set<String>> typeToRelationshipNamesMap = Map.of(type, Set.of(relationshipName));
        return new IntermediateParseResult(typeToIdsMap, typeToRelationshipNamesMap);
    }

    private IntermediateParseResult parseData(JsonNode rootNode,
                                              Function<JsonNode, IntermediateParseResult> dataProcessor) {
        if (rootNode == null || !rootNode.isObject() || rootNode.get("data") == null) {
            throw new InvalidJsonApiResponse("Json:Api response must contain top-level 'data' member");
        }
        JsonNode dataNode = rootNode.get("data");
        return dataProcessor.apply(dataNode);
    }

    private IntermediateParseResult processRelationshipsNode(JsonNode dataObjectNode) {
        LOG.debug("Processing relationships for the resource: {} - {}", dataObjectNode.get("type").asText(), dataObjectNode.get("id").asText());
        JsonNode relationshipsNode = dataObjectNode.get("relationships");
        if (relationshipsNode == null || !relationshipsNode.isObject()) {
            return new IntermediateParseResult(Collections.emptyMap(), Collections.emptyMap());
        }
        Map<String, Set<String>> typeToIdsMap = new HashMap<>();
        Map<String, Set<String>> typeToRelationshipNamesMap = new HashMap<>();
        for (Iterator<String> it = relationshipsNode.fieldNames(); it.hasNext(); ) {
            String relationshipName = it.next();
            LOG.debug("Processing relationship: " + relationshipName);
            JsonNode relationshipNode = relationshipsNode.get(relationshipName);
            if (relationshipNode != null && relationshipNode.isObject()) {
                JsonNode relationshipDataNode = relationshipNode.get("data");
                processObjectOrArray(
                        relationshipDataNode,
                        n -> processRelationshipDataNode(n, relationshipName, typeToIdsMap, typeToRelationshipNamesMap)
                );
            }
        }
        return new IntermediateParseResult(
                Collections.unmodifiableMap(typeToIdsMap),
                Collections.unmodifiableMap(typeToRelationshipNamesMap)
        );
    }

    private void processObjectOrArray(JsonNode node, Consumer<JsonNode> consumer) {
        if (node != null && node.isObject()) {
            consumer.accept(node);
        } else if (node != null && node.isArray()) {
            node.forEach(consumer);
        }
    }

    private void processRelationshipDataNode(JsonNode relationshipDataNode,
                                             String relationshipName,
                                             Map<String, Set<String>> typeToIdsMap,
                                             Map<String, Set<String>> typeToRelationshipNamesMap) {
        IdAndType idAndType = getTypeAndId(relationshipDataNode);
        if (idAndType != null) {
            LOG.debug("Processing data entry: {} - {}", idAndType.type, idAndType.id);
            // save type - ids relation
            if (typeToIdsMap.containsKey(idAndType.type)) {
                typeToIdsMap.get(idAndType.type).add(idAndType.id);
            } else {
                Set<String> ids = new HashSet<>();
                ids.add(idAndType.id);
                typeToIdsMap.put(idAndType.type, ids);
            }
            // save type - rel relation
            if (typeToRelationshipNamesMap.containsKey(idAndType.type)) {
                typeToRelationshipNamesMap.get(idAndType.type).add(relationshipName);
            } else {
                Set<String> relationshipNames = new HashSet<>();
                relationshipNames.add(relationshipName);
                typeToRelationshipNamesMap.put(idAndType.type, relationshipNames);
            }
        }
    }

    private IdAndType getTypeAndId(JsonNode relationshipDataNode) {
        String id = readStringValue(relationshipDataNode, "id");
        String type = readStringValue(relationshipDataNode, "type");
        return id == null || type == null ? null : new IdAndType(id, type);
    }

    private String readStringValue(JsonNode node, String fieldName) {
        JsonNode idNode = node.get(fieldName);
        if (idNode != null && idNode.isTextual()) {
            return idNode.asText();
        } else {
            return null;
        }
    }

    private record IdAndType(String id, String type) {

    }

    public record IntermediateParseResult(Map<String, Set<String>> typeToIdsMap,
                                           Map<String, Set<String>> typeToRelationshipNamesMap) {
    }

}
