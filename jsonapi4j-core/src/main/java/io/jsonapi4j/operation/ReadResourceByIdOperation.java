package io.jsonapi4j.operation;

import io.jsonapi4j.request.JsonApiRequest;
import io.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;

/**
 * Implement this interface to let jsonapi4j framework to know how to read a resource by id.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#fetching-resources">resource fetching</a>.
 * Available under GET /{resourceType}/{resourceId}. Returns 200 if executed successfully.
 * <p>
 * If jsonapi4j framework is not managed to find the corresponding resource {@link ReadMultipleResourcesOperation} in
 * {@link OperationsRegistry} it automatically tries to fall back to {@link ReadResourceByIdOperation} that mimics
 * this operation by using a sequential bruteforce approach that reads resources one by one which is far from being ideal.
 * <p>
 * Main logic is supposed to be implemented in {@link #readById(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #readById(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link io.jsonapi4j.model.document.error.ErrorsDoc}.
 *
 * @param <RESOURCE_DTO> a downstream object type that encapsulates internal model implementation and of this
 *                       JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 */
public interface ReadResourceByIdOperation<RESOURCE_DTO> extends ResourceOperation {

    /**
     * Reads a single resource.
     *
     * @param request incoming {@link JsonApiRequest}
     * @return {@link RESOURCE_DTO} instance that relates to the current resource
     */
    RESOURCE_DTO readById(JsonApiRequest request);

    /**
     * Overrides NO-OP implementation by {@link ReadResourceByIdOperation}-specific default validations, namely it
     * checks that the request has the corresponding resource id in the URL (for example, /users/{userId}) and checks
     * that it's a valid string.
     * <p>
     * Thus, it's recommended to call
     * <pre>
     *     {@code
     *     ReadResourceByIdOperation.super.validate(request);
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
        new JsonApi4jDefaultValidator().validateResourceId(request.getResourceId());
    }

}
