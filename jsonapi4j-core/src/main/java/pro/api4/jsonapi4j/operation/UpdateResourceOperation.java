package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

/**
 * Implement this interface to let jsonapi4j framework to know how to update the given resource.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-updating">resource updating</a>.
 * Available under PATCH /{resourceType}/{resourceId}. {@link SingleResourceDoc} is
 * expected as a payload - with a full or limited attributes section. Can also update resource's relationships.
 * Based on the specification the server must either perform a complete replacement of the relationship resource
 * linkage object(s) or return 403 Forbidden.
 * Returns 202 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #update(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters, payload and their formats must be done there.
 * Don't bring these checks to the {@link #update(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface UpdateResourceOperation extends ResourceOperation {

    /**
     * Updates the given resource: attributes section, relationships, or both.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void update(JsonApiRequest request);

    /**
     * Overrides NO-OP implementation by {@link UpdateResourceOperation}-specific default validations, namely it
     * checks that the request has the corresponding resource id in the URL (for example, /users/{userId}) and checks
     * that it's a valid string.
     * <p>
     * Thus, it's recommended to call
     * <pre>
     *     {@code
     *     UpdateResourceOperation.super.validate(request);
     *     }
     * </pre>
     * at the beginning of the custom method implementation.
     * <p>
     * Custom implementation usually should focus mostly on the payload validation.
     * <p>
     * Must throw an exception if validation failed. Check DefaultErrorHandlerFactory, Jsr380ErrorHandlers,
     * etc. for more details.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    @Override
    default void validate(JsonApiRequest request) {
        new JsonApi4jDefaultValidator().validateResourceId(request.getResourceId());
    }

}
