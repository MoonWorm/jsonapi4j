package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource operations.
 */
public interface ResourceOperation extends Operation {

    /**
     * @return type of the given JSON:API resource
     */
    ResourceType resourceType();

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
        throw new ResourceNotFoundException(request.getResourceId(), resourceType());
    }

    /**
     * Throws an exception that tells the framework that an item of the given resource of the given id is not found.
     *
     * @param id explicitly requested resource ID value
     * @throws ResourceNotFoundException if triggered
     */
    default void throwResourceNotFoundException(String id) throws ResourceNotFoundException {
        throw new ResourceNotFoundException(id, resourceType());
    }

}
