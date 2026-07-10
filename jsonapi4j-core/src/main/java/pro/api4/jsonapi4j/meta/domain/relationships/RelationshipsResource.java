package pro.api4.jsonapi4j.meta.domain.relationships;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

import java.text.MessageFormat;

@JsonApiResource(resourceType = RelationshipsResource.RELATIONSHIPS)
public class RelationshipsResource implements Resource<RelationshipsResource.RelationshipDescriptorAttributes> {

    public static final String RELATIONSHIPS = "relationships";

    public static String relationshipId(RelationshipDescriptorAttributes a) {
        return MessageFormat.format("{0}.{1}", a.parentResourceType(), a.name());
    }

    @Override
    public String resolveResourceId(RelationshipDescriptorAttributes a) {
        return relationshipId(a);
    }

    @Override
    public Object resolveAttributes(RelationshipDescriptorAttributes a) {
        return a;
    }

    public record RelationshipDescriptorAttributes(String name,
                                                   String parentResourceType,
                                                   String relationshipType,
                                                   String className) {
    }
}
