package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Composite operation interface that covers all to-many relationship operations.
 * <p>
 * Implement this interface instead of the individual to-many relationship operation interfaces
 * ({@link ReadToManyRelationshipOperation}, {@link UpdateToManyRelationshipOperation},
 * {@link AddToManyRelationshipOperation}, {@link DeleteToManyRelationshipOperation}) when you want
 * to group them in a single class. Each operation method throws
 * {@link pro.api4.jsonapi4j.operation.exception.OperationNotFoundException} by default, so you only need
 * to override the operations your relationship actually supports.
 * <p>
 * The {@link #validate(pro.api4.jsonapi4j.request.JsonApiRequest)} method automatically
 * dispatches to the appropriate per-operation validation hook based on the operation type.
 * Override any of the {@code validateXxx} methods to add operation-specific validation logic:
 * <ul>
 *   <li>{@link #validateReadToMany(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateUpdateToMany(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateAddToMany(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateDeleteFromToMany(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 * </ul>
 *
 * @param <RESOURCE_DTO>     the downstream object type for the parent resource
 * @param <RELATIONSHIP_DTO> the downstream object type for each relationship member's resource linkage
 * @see ReadToManyRelationshipOperation
 * @see UpdateToManyRelationshipOperation
 * @see AddToManyRelationshipOperation
 * @see DeleteToManyRelationshipOperation
 */
public interface ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>,
        UpdateToManyRelationshipOperation,
        AddToManyRelationshipOperation,
        DeleteToManyRelationshipOperation {

    @Override
    default PaginationAwareResponse<RELATIONSHIP_DTO> readMany(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.UPDATE_TO_MANY_RELATIONSHIPS,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void add(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.ADD_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void delete(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.DELETE_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_MANY_RELATIONSHIP -> validateReadToMany(request);
            case UPDATE_TO_MANY_RELATIONSHIPS -> validateUpdateToMany(request);
            case ADD_TO_MANY_RELATIONSHIP -> validateAddToMany(request);
            case DELETE_TO_MANY_RELATIONSHIP -> validateDeleteFromToMany(request);
        }
    }

    /**
     * Validation hook called before {@link #readMany(JsonApiRequest)}.
     * Override to add validation logic specific to to-many relationship read requests.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateReadToMany(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #update(JsonApiRequest)} for to-many relationships.
     * Override to add validation logic specific to complete replacement of the relationship members.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateUpdateToMany(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #add(JsonApiRequest)}.
     * Override to add validation logic specific to adding new members to the relationship.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateAddToMany(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #delete(JsonApiRequest)} for to-many relationships.
     * Override to add validation logic specific to removing members from the relationship.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateDeleteFromToMany(JsonApiRequest request) {
    }

}
