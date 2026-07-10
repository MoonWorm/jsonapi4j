package pro.api4.jsonapi4j.meta.operation.relationships;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource.RelationshipDescriptorAttributes;

import java.util.List;
import java.util.Optional;

public interface RelationshipsIntrospector {

    List<RelationshipDescriptorAttributes> relationships();

    Optional<RelationshipDescriptorAttributes> relationshipById(String id);

    List<Ref> relationshipRefs();

}
