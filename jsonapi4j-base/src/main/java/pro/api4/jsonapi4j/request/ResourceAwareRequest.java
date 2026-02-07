package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.CreateResourceOperation;

public interface ResourceAwareRequest {

    /**
     * Relevant for all operations except for creation of a new resource, e.g. POST /countries.
     * <p>
     * For all other operations will hold an id of a given resource e.g. GET /countries/FI resource id is "FI".
     *
     * @return resource id - never  <code>null</code> for all operation, except for {@link CreateResourceOperation}
     */
    String getResourceId();

    /**
     * Relevant for all operations, e.g. GET /countries/NO/relationships/currencies or GET /countries?filter[id]=SE,NO
     * <p>
     * This will hold the {@link ResourceType} representation of the 'countries' resource.
     *
     * @return resource type representation, never <code>null</code>
     */
    ResourceType getTargetResourceType();

}
