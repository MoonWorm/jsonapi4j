package pro.api4.jsonapi4j.domain;

/**
 * Distinguishes the cardinality of a JSON:API relationship.
 *
 * @see pro.api4.jsonapi4j.domain.ToOneRelationship
 * @see pro.api4.jsonapi4j.domain.ToManyRelationship
 */
public enum RelationshipType {
    /** The relationship has a single resource linkage object in its {@code "data"} member. */
    TO_ONE,
    /** The relationship has an array of resource linkage objects in its {@code "data"} member. */
    TO_MANY
}
