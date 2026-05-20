package pro.api4.jsonapi4j.init;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

import java.io.IOException;

import static pro.api4.jsonapi4j.model.document.data.RelationshipObject.DATA_FIELD;
import static pro.api4.jsonapi4j.model.document.data.RelationshipObject.LINKS_FIELD;
import static pro.api4.jsonapi4j.model.document.data.RelationshipObject.META_FIELD;

public class RelationshipObjectDeserializer extends StdDeserializer<RelationshipObject> {

    public RelationshipObjectDeserializer() {
        super(RelationshipObject.class);
    }

    @Override
    public RelationshipObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.has(DATA_FIELD)) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode data = node.get(DATA_FIELD);
            if (data.isArray()) {
                return mapper.treeToValue(node, ToManyRelationshipObject.class);
            } else {
                return mapper.treeToValue(node, ToOneRelationshipObject.class);
            }
        }
        LinksObject links = node.has(LINKS_FIELD)
                ? p.getCodec().treeToValue(node.get(LINKS_FIELD), LinksObject.class)
                : null;
        Object meta = node.has(META_FIELD)
                ? p.getCodec().treeToValue(node.get(META_FIELD), Object.class)
                : null;
        return new RelationshipObject(links, meta);
    }

}
