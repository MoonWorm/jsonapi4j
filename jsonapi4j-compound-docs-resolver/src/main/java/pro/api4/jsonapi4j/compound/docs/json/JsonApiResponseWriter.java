package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pro.api4.jsonapi4j.compound.docs.exception.InvalidJsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class JsonApiResponseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonApiResponseWriter.class);

    private final ObjectMapper objectMapper;

    public JsonApiResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String composeWithIncludedMember(ObjectNode rootNode, Set<String> resources) {
        ArrayNode includedNode = objectMapper.createArrayNode();
        resources.forEach(r -> {
            try {
                JsonNode resourceAsNode = objectMapper.readTree(r);
                includedNode.add(resourceAsNode);
            } catch (JsonProcessingException e) {
                LOGGER.error("JSON:API is not a valid JSON, can't read a JSON tree", e);
            }
        });

        rootNode.set("included", includedNode);

        try {
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonApiResponse("Can't compose the final Json:Api response", e);
        }
    }

}
