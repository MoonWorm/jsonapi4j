package io.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

public record ParseResult(Map<String, Set<String>> typeToIdsMap,
                          Map<String, Set<String>> typeToRelationshipNameMap,
                          JsonNode rootNode) {
}
