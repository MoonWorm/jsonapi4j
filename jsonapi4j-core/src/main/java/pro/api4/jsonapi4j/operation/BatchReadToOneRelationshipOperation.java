package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;
import java.util.Map;

/**
 * This is an advanced concept - it's not required to make the framework works for main scenarios. It only helps
 * to streamline a relationship resource linkages resolution. Works for the situations when the client querying
 * multiple primary resources and requests to include this relationship. If framework finds this interface implementation
 * for the given relationship it tries to resolve {@link RELATIONSHIP_DTO} for all {@link RESOURCE_DTO} in batch. This
 * approach is basically generates 1 request instead of N per relationship (where N is a number of primary resources).
 * <p>
 * If you choose to implement a batch operation - there is no longer need to implement a dedicated
 * {@link ReadToOneRelationshipOperation}. The framework will use a batch version for all scenarios.
 *
 * @param <RESOURCE_DTO>     a downstream object type that encapsulates internal model implementation and of this
 *                           JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 * @param <RELATIONSHIP_DTO> downstream dto object type that represents type of relationship's resource identifiers
 */
public interface BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>
        extends ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> {

    /**
     * Reads a resource linkage objects that relates to this relationship in batch.
     * If no relationship exist returns <code>null</code> {@link RELATIONSHIP_DTO}
     *
     * @param originalRequest incoming {@link JsonApiRequest}
     * @param resourceDtos    contextual list of primary resource's {@link RESOURCE_DTO}
     * @return map of {@link RESOURCE_DTO} - {@link RELATIONSHIP_DTO} pairs
     */
    Map<RESOURCE_DTO, RELATIONSHIP_DTO> readBatches(JsonApiRequest originalRequest,
                                                              List<RESOURCE_DTO> resourceDtos);

}
