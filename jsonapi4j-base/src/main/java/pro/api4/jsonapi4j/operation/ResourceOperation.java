package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource operations.
 */
public interface ResourceOperation extends Operation {

    @Override
    default void validate(JsonApiRequest request) {

    }

}
