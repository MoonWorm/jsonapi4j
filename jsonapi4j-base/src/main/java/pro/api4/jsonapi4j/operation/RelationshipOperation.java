package pro.api4.jsonapi4j.operation;

/**
 * Base interface for all relationship operations, covering both to-one and to-many variants.
 * <p>
 * Extends {@link ResourceOperation} (and transitively {@link Operation}).
 * Not intended to be implemented directly. Use the specific operation interfaces
 * ({@link ReadToOneRelationshipOperation}, {@link ReadToManyRelationshipOperation},
 * {@link UpdateToOneRelationshipOperation}, {@link UpdateToManyRelationshipOperation},
 * {@link AddToManyRelationshipOperation}, {@link DeleteToManyRelationshipOperation})
 * or the composite {@link ToOneRelationshipOperations} / {@link ToManyRelationshipOperations}.
 */
public interface RelationshipOperation extends ResourceOperation {

}
