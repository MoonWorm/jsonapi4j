package io.jsonapi4j.processor.exception;

import io.jsonapi4j.domain.ResourceType;

/**
 * Can be explicitly thrown from the operation when the resource is not found in a downstream service.
 */
public class ResourceNotFoundException extends DataRetrievalException {

    public ResourceNotFoundException(String id, ResourceType resourceType) {
        super("'" + resourceType.getType() + "' resource of a given id (" + id + ") is not found");
    }

}
