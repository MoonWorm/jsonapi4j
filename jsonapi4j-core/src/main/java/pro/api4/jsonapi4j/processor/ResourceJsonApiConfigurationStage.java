package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
public abstract class ResourceJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;

    // relationships
    private final Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers;
    private final Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toManyRelationshipResolvers;
    private final Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toOneRelationshipResolvers;
    private final Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToOneRelationshipResolvers;
    private final Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToManyRelationshipResolvers;
    private final Map<RelationshipName, RelationshipType> relationshipTypes;

    // resource links
    private ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver;

    // resource meta
    private ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver;

    // resource type and id
    private ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver;

    protected ResourceJsonApiConfigurationStage(REQUEST request) {
        this.request = request;

        this.defaultRelationshipResolvers = new HashMap<>();
        this.toManyRelationshipResolvers = new HashMap<>();
        this.toOneRelationshipResolvers = new HashMap<>();
        this.batchToOneRelationshipResolvers = new HashMap<>();
        this.batchToManyRelationshipResolvers = new HashMap<>();
        this.relationshipTypes = new HashMap<>();
    }

    protected void toManyRelationshipResolverInternal(RelationshipName relationshipName,
                                                      ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> resolver) {
        log.info("Configuring basic multi data relationship resolver for: {}", relationshipName);
        if (!defaultRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("Unknown relationship. Declare default relationship first.");
        }
        if (batchToManyRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("There is a batch resolver already registered for this relationship. Batch resolvers have higher priority.");
        }
        relationshipTypes.put(relationshipName, RelationshipType.TO_MANY);
        if (relationshipsRequested(relationshipName.getName())) {
            toManyRelationshipResolvers.put(relationshipName, resolver);
        }
    }

    protected void toOneRelationshipResolverInternal(RelationshipName relationshipName,
                                                     ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> resolver) {
        log.info("Configuring basic to one relationship resolver for: {}", relationshipName);
        if (!defaultRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("Unknown relationship. Declare default relationship first.");
        }
        if (batchToOneRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("There is a batch resolver already registered for this relationship. Batch resolvers have higher priority.");
        }
        relationshipTypes.put(relationshipName, RelationshipType.TO_ONE);
        if (relationshipsRequested(relationshipName.getName())) {
            toOneRelationshipResolvers.put(relationshipName, resolver);
        }
    }

    protected void batchToManyRelationshipResolverInternal(RelationshipName relationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToManyRelationshipResolver) {
        log.info("Configuring batch multi data relationship resolver for: {}", relationshipName);
        if (!defaultRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("Unknown relationship. Declare default relationship first.");
        }

        ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> removed = toManyRelationshipResolvers.remove(relationshipName);
        if (removed != null) {
            log.warn("Removing basic toManyRelationshipResolver for relationship [{}] since batch one has higher priority.", relationshipName);
        }

        relationshipTypes.put(relationshipName, RelationshipType.TO_MANY);
        if (relationshipsRequested(relationshipName.getName())) {
            this.batchToManyRelationshipResolvers.put(relationshipName, batchToManyRelationshipResolver);
        }
    }

    protected void batchToOneRelationshipResolverInternal(RelationshipName relationshipName,
                                                          BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToOneRelationshipResolver) {
        log.info("Configuring batch to one relationship resolver for: {}", relationshipName);
        if (!defaultRelationshipResolvers.containsKey(relationshipName)) {
            throw new IllegalStateException("Unknown relationship. Declare default relationship first.");
        }

        ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> removed = toOneRelationshipResolvers.remove(relationshipName);
        if (removed != null) {
            log.warn("Removing basic toOneRelationshipResolver for relationship [{}] since batch one has higher priority.", relationshipName);
        }

        relationshipTypes.put(relationshipName, RelationshipType.TO_ONE);
        if (relationshipsRequested(relationshipName.getName())) {
            this.batchToOneRelationshipResolvers.put(relationshipName, batchToOneRelationshipResolver);
        }
    }

    protected void setResourceLinksResolverInternal(ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver) {
        this.resourceLinksResolver = resourceLinksResolver;
    }

    protected void setResourceMetaResolverInternal(ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver) {
        this.resourceMetaResolver = resourceMetaResolver;
    }

    protected void setResourceTypeAndIdResolverInternal(
            ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdSupplier
    ) {
        Validate.notNull(resourceTypeAndIdSupplier);
        this.resourceTypeAndIdResolver = resourceTypeAndIdSupplier;
    }

    protected boolean relationshipsRequested(String relationship) {
        if (request instanceof IncludeAwareRequest) {
            return ((IncludeAwareRequest) request).requested(relationship);
        }
        return false;
    }

}
