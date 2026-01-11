package pro.api4.jsonapi4j.domain;

import lombok.*;
import pro.api4.jsonapi4j.processor.RelationshipType;

import java.util.Map;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class RegisteredRelationship<T extends Relationship<?>> {

    private T relationship;
    private ResourceType parentResourceType;
    private RelationshipName relationshipName;
    private RelationshipType relationshipType;
    private Map<String, Object> pluginInfo;

}
