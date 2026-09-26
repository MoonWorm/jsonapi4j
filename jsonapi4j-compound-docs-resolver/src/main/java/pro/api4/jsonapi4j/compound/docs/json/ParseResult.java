package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.databind.JsonNode;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Map;
import java.util.Set;

/**
 * @param relationships relationship name to the resources the primary data links to; for a relationship document
 *                      this holds the requested relationship itself
 * @param rootNode      the parsed top-level document
 */
public record ParseResult(Map<String, Set<IdAndType>> relationships, JsonNode rootNode) {
}
