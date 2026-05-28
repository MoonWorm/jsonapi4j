package pro.api4.jsonapi4j.exception;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

/**
 * Signals that the requested resource does not exist in the downstream data source (HTTP 404).
 * <p>
 * Throw this from an operation implementation (e.g. {@code readById}, {@code updateResource})
 * when the downstream service returns no result for the given identifier. The framework
 * converts it to a JSON:API {@code ErrorsDoc} with a {@code 404 Not Found} status.
 */
public class ResourceNotFoundException extends JsonApi4jException {

    public ResourceNotFoundException(String detail) {
        super(
                HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode(),
                DefaultErrorCodes.NOT_FOUND,
                detail
        );
    }

    public ResourceNotFoundException(String id,
                                     ResourceType resourceType) {
        this(String.format("'%s' resource of a given id (%s) is not found", resourceType.getType(), id));
    }

}
