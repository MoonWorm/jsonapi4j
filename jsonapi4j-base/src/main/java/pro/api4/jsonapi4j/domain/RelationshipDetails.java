package pro.api4.jsonapi4j.domain;

import lombok.Data;

/**
 * Combines a {@link RelationshipName} and a {@link RelationshipType} into a single value object
 * that uniquely identifies a relationship within a resource domain.
 * <p>
 * Used by the framework when registering resolver configuration in
 * {@link pro.api4.jsonapi4j.processor.ResourceJsonApiContext} to record which relationships
 * have resolvers configured, and to perform to-one vs. to-many resolver lookups.
 */
@Data
public class RelationshipDetails {

    /** The name of the relationship (e.g. {@code "citizenships"}, {@code "placeOfBirth"}). */
    private final RelationshipName relationshipName;

    /** Whether this is a to-one or to-many relationship. */
    private final RelationshipType relationshipType;
}
