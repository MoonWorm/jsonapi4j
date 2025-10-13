package io.jsonapi4j.operation;

import io.jsonapi4j.processor.exception.ResourceNotFoundException;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.plugin.OperationPluginAware;
import io.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource operations.
 */
public interface ResourceOperation extends OperationPluginAware {

    /**
     * @return type of the given JSON:API resource
     */
    ResourceType resourceType();

    /**
     * It is recommended to complement the main logic implementation with some validation logic by implementing
     * this method. This method invoked before the main logic execution.
     * All checks regarding input parameters, payload and their formats must be done there.
     * Don't bring these checks to the main logic. If some error detected thrown exception will be
     * automatically processed and transformed into a valid {@link io.jsonapi4j.model.document.error.ErrorsDoc}.
     * <p>
     * By default, no validation implemented.
     * <p>
     * Must throw an exception if validation failed. Check DefaultErrorHandlerFactory, Jsr380ErrorHandlers,
     * etc. for more details.
     *
     * @param request incoming {@link JsonApiRequest}
     */
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
