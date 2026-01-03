package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource operations.
 */
public interface ResourceOperation extends Operation {

    @Override
    default void validate(JsonApiRequest request) {

    }

    /**
     * Throws an exception that tells the framework that an item of the given resource of the given id is not found.
     * Gets requested ID from a request (resource id)
     *
     * @param request {@link JsonApiRequest}
     * @throws ResourceNotFoundException if triggered
     */
    default void throwResourceNotFoundException(JsonApiRequest request) throws ResourceNotFoundException {
        throw new ResourceNotFoundException(request.getResourceId(), request.getTargetResourceType());
    }

}
