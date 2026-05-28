package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource CRUD operations.
 * <p>
 * Extends {@link Operation} and overrides {@link #validate(pro.api4.jsonapi4j.request.JsonApiRequest)}
 * with a no-op default, so individual resource operation interfaces can opt in to validation
 * by overriding it.
 * <p>
 * Not intended to be implemented directly. Use the specific operation interfaces
 * ({@link ReadResourceByIdOperation}, {@link ReadMultipleResourcesOperation},
 * {@link CreateResourceOperation}, {@link UpdateResourceOperation},
 * {@link DeleteResourceOperation}) or the composite {@link ResourceOperations}.
 */
public interface ResourceOperation extends Operation {

    @Override
    default void validate(JsonApiRequest request) {

    }

}
