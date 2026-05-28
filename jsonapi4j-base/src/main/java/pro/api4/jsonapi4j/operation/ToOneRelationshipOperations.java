package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Composite operation interface that covers all to-one relationship operations.
 * <p>
 * Implement this interface instead of the individual to-one relationship operation interfaces
 * ({@link ReadToOneRelationshipOperation}, {@link UpdateToOneRelationshipOperation}) when you want
 * to group them in a single class. Each operation method throws
 * {@link pro.api4.jsonapi4j.operation.exception.OperationNotFoundException} by default, so you only need
 * to override the operations your relationship actually supports.
 * <p>
 * The {@link #validate(pro.api4.jsonapi4j.request.JsonApiRequest)} method automatically
 * dispatches to the appropriate per-operation validation hook based on the operation type.
 * Override any of the {@code validateXxx} methods to add operation-specific validation logic:
 * <ul>
 *   <li>{@link #validateReadToOne(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateUpdateToOne(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 * </ul>
 *
 * @param <RESOURCE_DTO>     the downstream object type for the parent resource
 * @param <RELATIONSHIP_DTO> the downstream object type for the relationship's resource linkage
 * @see ReadToOneRelationshipOperation
 * @see UpdateToOneRelationshipOperation
 */
public interface ToOneRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        UpdateToOneRelationshipOperation, ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> {

    @Override
    default RELATIONSHIP_DTO readOne(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_ONE_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.UPDATE_TO_ONE_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_ONE_RELATIONSHIP -> validateReadToOne(request);
            case UPDATE_TO_ONE_RELATIONSHIP -> validateUpdateToOne(request);
        }
    }

    /**
     * Validation hook called before {@link #readOne(JsonApiRequest)}.
     * Override to add validation logic specific to to-one relationship read requests.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateReadToOne(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #update(JsonApiRequest)} for to-one relationships.
     * Override to add validation logic specific to to-one relationship update requests.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateUpdateToOne(JsonApiRequest request) {
    }

}
