package io.jsonapi4j.operation;

import io.jsonapi4j.request.JsonApiRequest;

/**
 * Implement this interface to let jsonapi4j framework to know how to create a new resource.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-creating">resource creation</a>.
 * Available under POST /{resourceType}. {@link io.jsonapi4j.model.document.data.SingleResourceDoc} is
 * expected as a payload - with a full attributes section. Can also create resource's relationships linkages.
 * Returns 201 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #create(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters, payload and their formats must be done there.
 * Don't bring these checks to the {@link #create(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link io.jsonapi4j.model.document.error.ErrorsDoc}.
 * Custom implementation of the validation logic usually should focus mostly on the payload validation.
 */
public interface CreateResourceOperation<RESOURCE_DTO> extends ResourceOperation {

    /**
     * Create a new resource: attributes section with relationships or without.
     *
     * @param request incoming {@link JsonApiRequest}
     * @return newly created and saved {@link RESOURCE_DTO}
     */
    RESOURCE_DTO create(JsonApiRequest request);

}
