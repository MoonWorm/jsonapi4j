package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.function.Consumer;

/**
 * Base interface for all relationship operations: both ToOne* and ToMany*.
 */
public interface RelationshipOperation extends ResourceOperation {

    Consumer<JsonApiRequest> DEFAULT_VALIDATOR = request -> {
        new JsonApi4jDefaultValidator().validateResourceId(request.getResourceId());
    };

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
    @Override
    default void validate(JsonApiRequest request) {
        DEFAULT_VALIDATOR.accept(request);
    }

}
