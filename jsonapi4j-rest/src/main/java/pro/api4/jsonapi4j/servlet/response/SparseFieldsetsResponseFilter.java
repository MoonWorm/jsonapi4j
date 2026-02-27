package pro.api4.jsonapi4j.servlet.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class SparseFieldsetsResponseFilter {

    private SparseFieldsetsResponseFilter() {

    }

    public static Object apply(Object responseBody,
                               Map<String, Set<String>> sparseFieldsets,
                               ObjectMapper objectMapper) {
        if (responseBody == null || MapUtils.isEmpty(sparseFieldsets)) {
            return responseBody;
        }
        JsonNode rootNode = objectMapper.valueToTree(responseBody);
        apply(rootNode, sparseFieldsets);
        return rootNode;
    }

    public static String apply(String responseBody,
                               Map<String, Set<String>> sparseFieldsets,
                               ObjectMapper objectMapper) {
        if (responseBody == null || MapUtils.isEmpty(sparseFieldsets)) {
            return responseBody;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            apply(rootNode, sparseFieldsets);
            return objectMapper.writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply sparse fieldsets to JSON:API response", e);
        }
    }

    static void apply(JsonNode rootNode,
                      Map<String, Set<String>> sparseFieldsets) {
        if (rootNode == null || !rootNode.isObject() || MapUtils.isEmpty(sparseFieldsets)) {
            return;
        }
        applyForDataMember(rootNode.get("data"), sparseFieldsets);
        JsonNode includedNode = rootNode.get("included");
        if (includedNode != null && includedNode.isArray()) {
            for (JsonNode node : includedNode) {
                applyForSingleResource(node, sparseFieldsets);
            }
        }
    }

    private static void applyForDataMember(JsonNode dataNode,
                                           Map<String, Set<String>> sparseFieldsets) {
        if (dataNode == null || dataNode.isNull()) {
            return;
        }
        if (dataNode.isObject()) {
            applyForSingleResource(dataNode, sparseFieldsets);
            return;
        }
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                applyForSingleResource(node, sparseFieldsets);
            }
        }
    }

    private static void applyForSingleResource(JsonNode resourceNode,
                                               Map<String, Set<String>> sparseFieldsets) {
        if (resourceNode == null || !resourceNode.isObject()) {
            return;
        }
        JsonNode typeNode = resourceNode.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            return;
        }
        Set<String> requestedFields = sparseFieldsets.getOrDefault(typeNode.asText(), Collections.emptySet());
        if (!sparseFieldsets.containsKey(typeNode.asText())) {
            return;
        }
        ObjectNode resourceObjectNode = (ObjectNode) resourceNode;
        filterObjectNodeFields(resourceObjectNode, "attributes", requestedFields);
        filterObjectNodeFields(resourceObjectNode, "relationships", requestedFields);
    }

    private static void filterObjectNodeFields(ObjectNode resourceObjectNode,
                                               String memberName,
                                               Set<String> requestedFields) {
        JsonNode memberNode = resourceObjectNode.get(memberName);
        if (memberNode == null || !memberNode.isObject()) {
            return;
        }
        ObjectNode memberObjectNode = (ObjectNode) memberNode;
        for (Iterator<String> it = memberObjectNode.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            if (!requestedFields.contains(fieldName)) {
                it.remove();
            }
        }
        if (memberObjectNode.isEmpty()) {
            resourceObjectNode.remove(memberName);
        }
    }
}
