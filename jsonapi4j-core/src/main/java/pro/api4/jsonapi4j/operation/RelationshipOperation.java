package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.plugin.OperationPluginAware;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all relationship operations: both ToOne* and ToMany*.
 */
public interface RelationshipOperation extends OperationPluginAware {

    /**
     * @return name of the given relationship
     */
    RelationshipName relationshipName();

    /**
     * @return type of the parent JSON:API resource
     */
    ResourceType parentResourceType();

    /**
     * Checks that the request has the corresponding resource id in the URL
     * (for example, /users/{userId}/relationships/{relationshipName}) and checks
     * that it's a valid string.
     * <p>
     * Thus, it's recommended to call
     * <pre>
     *     {@code
     *     RelationshipOperation.super.validate(request);
     *     }
     * </pre>
     * at the beginning of the custom method implementation.
     * <p>
     * Can be overridden in any child ToMany* or ToOne* relationship operation.
     * <p>
     * Must throw an exception if validation failed. Check DefaultErrorHandlerFactory, Jsr380ErrorHandlers,
     * etc. for more details.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validate(JsonApiRequest request) {
        new JsonApi4jDefaultValidator().validateResourceId(request.getResourceId());
    }

}
