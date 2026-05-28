package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.RelationshipDetails;
import pro.api4.jsonapi4j.domain.RelationshipType;
import pro.api4.jsonapi4j.processor.resolvers.AttributesResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.Set;

/**
 * Base context class that aggregates all resolver functions configured for a resource
 * processing pipeline stage.
 * <p>
 * A context instance is built by the processor's fluent builder chain (the
 * {@code ConfigurationStage → JsonApiConfigurationStage → AttributesAwareStage → TerminalStage}
 * progression) and passed to every plugin visitor method call, giving plugins read-only access
 * to the configured resolvers for the current operation.
 * <p>
 * Subclasses add top-level doc resolvers specific to the single-resource
 * ({@link pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext}) or
 * multiple-resources
 * ({@link pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext}) pipeline.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 * @param <ATTRIBUTES>     the attributes object type
 */
@SuperBuilder
@Getter
public abstract class ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    /** Resolver that maps a resource DTO to its JSON:API {@code "id"} and {@code "type"} members. */
    private final ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver;

    /** Resolver that maps a resource DTO to the API-facing attributes object. */
    private final AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver;

    /**
     * Map of relationship name → default relationship resolver.
     * Used when a relationship needs to be included in the response but no specific
     * to-one or to-many resolver has been registered for it.
     */
    private final Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers;

    /** Map of relationship name → to-many relationship resolver (per-resource invocation). */
    private final Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toManyRelationshipResolvers;

    /** Map of relationship name → batch to-many relationship resolver (single call for all resources). */
    private final Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToManyRelationshipResolvers;

    /** Map of relationship name → to-one relationship resolver (per-resource invocation). */
    private final Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toOneRelationshipResolvers;

    /** Map of relationship name → batch to-one relationship resolver (single call for all resources). */
    private final Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToOneRelationshipResolvers;

    /** Set of relationships for which at least one resolver has been configured. */
    private final Set<RelationshipDetails> relationshipResolversConfiguredFor;

    /** Resolver for the {@code "links"} member of each individual resource object in the response. */
    private final ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver;

    /** Resolver for the {@code "meta"} member of each individual resource object in the response. */
    private final ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver;

    public boolean relationshipResolversConfiguredFor(RelationshipName relationshipName) {
        return relationshipResolversConfiguredFor(relationshipName, RelationshipType.TO_ONE)
                || relationshipResolversConfiguredFor(relationshipName, RelationshipType.TO_MANY);
    }

    public boolean relationshipResolversConfiguredFor(RelationshipName relationshipName, RelationshipType relationshipType) {
        return relationshipResolversConfiguredFor.contains(
                new RelationshipDetails(
                        relationshipName,
                        relationshipType
                )
        );
    }

}
