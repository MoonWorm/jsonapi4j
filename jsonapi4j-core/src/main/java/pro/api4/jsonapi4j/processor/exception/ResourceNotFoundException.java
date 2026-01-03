package pro.api4.jsonapi4j.processor.exception;

import pro.api4.jsonapi4j.domain.ResourceType;

/**
 * Can be explicitly thrown from the operation when the resource is not found in a downstream service.
 */
public class ResourceNotFoundException extends DataRetrievalException {

    public ResourceNotFoundException(String id, ResourceType resourceType) {
        super("'" + resourceType + "' resource of a given id (" + id + ") is not found");
    }

}
