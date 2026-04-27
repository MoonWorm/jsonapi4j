package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequest;

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
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 *
 * @param <RESOURCE_DTO> a downstream object type that encapsulates internal model implementation and of this
 *                       JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 */
public interface ReadResourceByIdOperation<RESOURCE_DTO> extends ResourceOperation {

    String READ_BY_ID_METHOD_NAME = "readById";

    /**
     * Reads a single resource.
     *
     * @param request incoming {@link JsonApiRequest}
     * @return {@link RESOURCE_DTO} instance that relates to the current resource
     */
    RESOURCE_DTO readById(JsonApiRequest request);

}
