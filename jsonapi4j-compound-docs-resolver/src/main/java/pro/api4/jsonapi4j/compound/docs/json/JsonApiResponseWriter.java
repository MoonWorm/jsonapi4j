package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pro.api4.jsonapi4j.compound.docs.IncludedGap;
import pro.api4.jsonapi4j.compound.docs.exception.InvalidJsonApiResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.model.document.BaseDoc;

import java.util.Collection;

public class JsonApiResponseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonApiResponseWriter.class);

    public static final String INCLUDED_INCOMPLETE_META_FIELD = "includedIncomplete";
    private static final String INCLUDED_FIELD = "included";
    private static final String GAP_REASON_FIELD = "reason";
    private static final String GAP_TYPE_FIELD = "type";

    private final ObjectMapper objectMapper;

    public JsonApiResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Adds {@code included} to {@code rootNode} when there are resources, and lists {@code gaps} under
     * {@value #INCLUDED_INCOMPLETE_META_FIELD} in its top-level {@code meta} when there are any - merged into the
     * document's own {@code meta}, which is created when absent and left untouched when it is not an object.
     */
    public String compose(ObjectNode rootNode, Collection<String> resources, Collection<IncludedGap> gaps) {
        if (!resources.isEmpty()) {
            rootNode.set(INCLUDED_FIELD, includedNode(resources));
        }
        if (!gaps.isEmpty()) {
            addGaps(rootNode, gaps);
        }
        try {
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonApiResponseException("Can't compose the final Json:Api response", e);
        }
    }

    private ArrayNode includedNode(Collection<String> resources) {
        ArrayNode includedNode = objectMapper.createArrayNode();
        resources.stream().sorted().forEach(r -> {
            try {
                includedNode.add(objectMapper.readTree(r));
            } catch (JsonProcessingException e) {
                LOGGER.error("JSON:API is not a valid JSON, can't read a JSON tree", e);
            }
        });
        return includedNode;
    }

    private void addGaps(ObjectNode rootNode, Collection<IncludedGap> gaps) {
        JsonNode metaNode = rootNode.get(BaseDoc.META_FIELD);
        if (metaNode != null && !metaNode.isObject()) {
            LOGGER.warn("Can't list incomplete included resources: the document's 'meta' is not an object");
            return;
        }
        ObjectNode meta = metaNode == null ? rootNode.putObject(BaseDoc.META_FIELD) : (ObjectNode) metaNode;
        ArrayNode gapsNode = meta.putArray(INCLUDED_INCOMPLETE_META_FIELD);
        gaps.stream().sorted().forEach(gap -> gapsNode.addObject()
                .put(GAP_REASON_FIELD, gap.reason().name())
                .put(GAP_TYPE_FIELD, gap.type()));
    }

}
