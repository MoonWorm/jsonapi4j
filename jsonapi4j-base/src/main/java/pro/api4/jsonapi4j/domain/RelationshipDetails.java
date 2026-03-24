package pro.api4.jsonapi4j.domain;

import lombok.Data;

@Data
public class RelationshipDetails {
    private final RelationshipName relationshipName;
    private final RelationshipType relationshipType;
}
