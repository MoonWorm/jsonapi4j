package pro.api4.jsonapi4j.exception;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

/**
 * Can be explicitly thrown from the operation when the resource is not found in a downstream service.
 */
public class ResourceNotFoundException extends JsonApi4jException {

    public ResourceNotFoundException(String id,
                                     ResourceType resourceType) {
        super(
                HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode(),
                DefaultErrorCodes.NOT_FOUND,
                String.format("'%s' resource of a given id (%s) is not found", resourceType.getType(), id)
        );
    }

}
