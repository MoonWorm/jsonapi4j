package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import java.util.function.Consumer;

/**
 * Implement this interface to let jsonapi4j framework to know how to delete a single resource by id.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-deleting">resource deletion</a>.
 * Available under DELETE /{resourceType}/{resourceId}. No payload expected. Returns 202 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #delete(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #delete(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface DeleteResourceOperation extends ResourceOperation {

    Consumer<JsonApiRequest> DEFAULT_VALIDATOR = request -> {
        new JsonApi4jDefaultValidator().validateResourceId(request.getResourceId());
    };

    /**
     * Deletes a single resource.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void delete(JsonApiRequest request);

    /**
     * Overrides NO-OP implementation by {@link DeleteResourceOperation}-specific default validations, namely it
     * checks that the request has the corresponding resource id in the URL (for example, /users/{userId}) and checks
     * that it's a valid string.
     * <p>
     * Thus, it's recommended to call
     * <pre>
     *     {@code
     *     DeleteResourceOperation.super.validate(request);
     *     }
     * </pre>
     * at the beginning of the custom method implementation.
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
