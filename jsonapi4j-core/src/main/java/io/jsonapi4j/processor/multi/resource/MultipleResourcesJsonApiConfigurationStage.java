package io.jsonapi4j.processor.multi.resource;

import io.jsonapi4j.processor.ResourceJsonApiConfigurationStage;
import io.jsonapi4j.processor.ResourceProcessorContext;
import io.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import io.jsonapi4j.processor.resolvers.AttributesResolver;
import io.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import io.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import io.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import io.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import io.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import io.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import io.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import io.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import io.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Map;

@Getter
@Setter
public class MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> extends ResourceJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> {

    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final ResourceProcessorContext processorContext;

    private MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

    public MultipleResourcesJsonApiConfigurationStage(REQUEST request,
                                                      MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                                      ResourceProcessorContext processorContext) {
        super(request);
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
    }

    public <ATTRIBUTES> MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver(
            AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver
    ) {
        return new MultipleResourcesAttributesAwareJsonApiConfigurationStage<>(
                this,
                attributesResolver
        );
    }

    /**
     * Configures doc-level 'links' object.
     * <p/>
     * Optional. Use if you want to populate doc-level {@link LinksObject} object.
     *
     * @param topLevelLinksResolver function that generates {@link LinksObject} by accepting the original request
     *                              object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver(
            MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver
    ) {
        this.topLevelLinksResolver = topLevelLinksResolver;
        return this;
    }

    /**
     * Configures doc-level 'meta' object.
     * <p/>
     * Optional. Use if you want to populate doc-level 'meta' object.
     *
     * @param topLevelMetaResolver function that generates free-form 'meta' Object by accepting the original request
     *                             object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver(
            MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver
    ) {
        this.topLevelMetaResolver = topLevelMetaResolver;
        return this;
    }

    /**
     * Declares all available relationships and the corresponding resolving functions. Default relationship resolvers
     * are not supposed to resolve relationship's 'data', only 'links' property. Default relationships represents
     * relationships of how they should look like if they weren't requested by using <b>include</b> query parameter.
     * <p/>
     * Must be configured before {@link #toOneRelationshipResolver(RelationshipName, ToOneRelationshipResolver)} and
     * {@link #toManyRelationshipResolver(RelationshipName, ToManyRelationshipResolver)}
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't.
     *
     * @param defaultRelationshipResolver resolving function
     * @param relationshipNames           vararg of target relationships
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> defaultRelationships(
            DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> defaultRelationshipResolver,
            RelationshipName... relationshipNames
    ) {
        Arrays.stream(relationshipNames)
                .forEach(r -> getDefaultRelationshipResolvers().put(r, defaultRelationshipResolver));
        return this;
    }

    /**
     * Declares all available relationships and the corresponding resolving functions. Default relationship resolvers
     * are not supposed to resolve relationship's 'data', only 'links' property. Default relationships represents
     * relationships of how they should look like if they weren't requested by using <b>include</b> query parameter.
     * <p/>
     * Must be configured before {@link #toOneRelationshipResolver(RelationshipName, ToOneRelationshipResolver)} and
     * {@link #toManyRelationshipResolver(RelationshipName, ToManyRelationshipResolver)}
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't.
     *
     * @param defaultRelationshipResolvers map of 'Relationship - Resolving function' pairs
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> defaultRelationships(
            Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers
    ) {
        getDefaultRelationshipResolvers().putAll(defaultRelationshipResolvers);
        return this;
    }

    /**
     * Registers a resolving function for the particular to-many relationship that previously declared by using
     * a {@link #defaultRelationships(DefaultRelationshipResolver, RelationshipName...)} method.
     * {@link ToManyRelationshipResolver} is called only if the corresponding relationship are requested by using
     * <b>include</b> query parameter. If not - sticks to the {@link DefaultRelationshipResolver}.
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't. But
     * either batch or simple resolver <b>MUST</b> be configured per each declared <b>default</b> relationship.
     *
     * @param relationshipName           target relationship
     * @param toManyRelationshipResolver function that resolves and generates a new instance of
     *                                   {@link ToManyRelationshipsDoc} by accepting the original request
     *                                   object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     * @throws IllegalStateException if the target relationship wasn't declared as 'default' relationship
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> toManyRelationshipResolver(
            RelationshipName relationshipName,
            ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> toManyRelationshipResolver
    ) {
        toManyRelationshipResolverInternal(relationshipName, toManyRelationshipResolver);
        return this;
    }

    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> toManyRelationshipResolvers(
            Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toManyRelationshipsResolver
    ) {
        for (Map.Entry<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : toManyRelationshipsResolver.entrySet()) {
            toManyRelationshipResolver(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Registers a resolving function for the particular to-one relationship that previously declared by using
     * a {@link #defaultRelationships(DefaultRelationshipResolver, RelationshipName...)} method.
     * {@link ToOneRelationshipResolver} is called only if the corresponding relationship are requested by using
     * <b>include</b> query parameter. If not - sticks to the {@link DefaultRelationshipResolver}.
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't. But
     * either batch or simple resolver <b>MUST</b> be configured per each declared <b>default</b> relationship.
     *
     * @param relationshipName          target relationship
     * @param toOneRelationshipResolver function that resolves and generates a new instance of
     *                                  {@link ToOneRelationshipDoc} by accepting the original request
     *                                  object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     * @throws IllegalStateException if the target relationship wasn't declared as 'default' relationship
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> toOneRelationshipResolver(
            RelationshipName relationshipName,
            ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> toOneRelationshipResolver
    ) {
        toOneRelationshipResolverInternal(relationshipName, toOneRelationshipResolver);
        return this;
    }

    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> toOneRelationshipResolvers(
            Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toOneRelationshipResolvers
    ) {
        for (Map.Entry<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : toOneRelationshipResolvers.entrySet()) {
            toOneRelationshipResolverInternal(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Registers a batch resolving function for the particular to-many relationship that previously declared by using
     * a {@link #defaultRelationships(DefaultRelationshipResolver, RelationshipName...)} method.
     * <p/>
     * {@link BatchToManyRelationshipResolver} is called only if the corresponding relationship are requested by using
     * <b>include</b> query parameter. If not - sticks to the {@link DefaultRelationshipResolver}.
     * <p/>
     * Batch resolving function might bring a significant performance improvement when requesting multiple resources.
     * Usually, relationship 'data' resolution implies separate network calls. By using a batch version we reduce
     * the number of network calls from N (per each requested resource) to 1.
     * <p/>
     * This feature is not relevant for {@link SingleResourceProcessor} because it's only dealing with 1 resource.
     * It's only needed for better consistency. If {@link BatchToManyRelationshipResolver} is already implemented and
     * used by {@link MultipleResourcesProcessor} there is no need to
     * implement a new dedicated relationship resolver and batch one can be simply reused for 1 resource.
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't. But
     * either batch or simple resolver <b>MUST</b> be configured per each declared <b>default</b> relationship.
     *
     * @param relationshipName                 target relationship
     * @param batchToManyRelationshipsResolver function that resolves and generates a new instance of
     *                                         {@link ToManyRelationshipsDoc} by accepting the original request
     *                                         object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     * @throws IllegalStateException if the target relationship wasn't declared as 'default' relationship
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> batchToManyRelationshipResolver(
            RelationshipName relationshipName,
            BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToManyRelationshipsResolver
    ) {

        batchToManyRelationshipResolverInternal(relationshipName, batchToManyRelationshipsResolver);
        return this;
    }

    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> batchToManyRelationshipResolvers(
            Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToManyRelationshipResolvers
    ) {
        for (Map.Entry<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : batchToManyRelationshipResolvers.entrySet()) {
            batchToManyRelationshipResolver(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Registers a batch resolving function for the particular to-one relationship that previously declared by using
     * a {@link #defaultRelationships(DefaultRelationshipResolver, RelationshipName...)} method.
     * <p/>
     * {@link BatchToOneRelationshipResolver} is called only if the corresponding relationship are requested by using
     * <b>include</b> query parameter. If not - sticks to the {@link DefaultRelationshipResolver}.
     * <p/>
     * Batch resolving function might bring a significant performance improvement when requesting multiple resources.
     * Usually, relationship 'data' resolution implies separate network calls. By using a batch version we reduce
     * the number of network calls from N (per each requested resource) to 1.
     * <p/>
     * This feature is not relevant for {@link SingleResourceProcessor} because it's only dealing with 1 resource.
     * It's only needed for better consistency. If {@link BatchToManyRelationshipResolver} is already implemented and
     * used by {@link MultipleResourcesProcessor} there is no need to
     * implement a new dedicated relationship resolver and batch one can be simply reused for 1 resource.
     * <p/>
     * Optional. Must be used for those resources that have some relationships. Can be omitted for those that don't. But
     * either batch or simple resolver <b>MUST</b> be configured per each declared <b>default</b> relationship.
     *
     * @param relationshipName               target relationship
     * @param batchToOneRelationshipResolver function that resolves and generates a new instance of
     *                                       {@link ToOneRelationshipDoc} by accepting the original request
     *                                       object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     * @throws IllegalStateException if the target relationship wasn't declared as 'default' relationship
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> batchToOneRelationshipResolver(
            RelationshipName relationshipName,
            BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToOneRelationshipResolver
    ) {
        batchToOneRelationshipResolverInternal(relationshipName, batchToOneRelationshipResolver);
        return this;
    }

    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> batchToOneRelationshipResolvers(
            Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToOneRelationshipResolvers
    ) {
        for (Map.Entry<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : batchToOneRelationshipResolvers.entrySet()) {
            batchToOneRelationshipResolver(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Configures resource-level 'links' object.
     * <p/>
     * Optional. Use if you want to populate a resource-level {@link LinksObject} object.
     *
     * @param resourceLinksResolver function that generates {@link LinksObject} by accepting the original request
     *                              object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver(
            ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver
    ) {
        setResourceLinksResolverInternal(resourceLinksResolver);
        return this;
    }

    /**
     * Configures resource-level 'meta' object.
     * <p/>
     * Optional. Use if you want to populate a resource-level 'meta' object.
     *
     * @param resourceMetaResolver function that generates free-form 'meta' Object by accepting the original request
     *                             object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver(
            ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver
    ) {
        setResourceMetaResolverInternal(resourceMetaResolver);
        return this;
    }

    /**
     * Sets resource type and resource id resolver.
     *
     * @param resourceTypeAndIdResolver - function that retrieves unique resource id and type from {@link DATA_SOURCE_DTO}
     * @return self ref
     */
    public MultipleResourcesTerminalStage<REQUEST, DATA_SOURCE_DTO, ?> resourceTypeAndIdResolver(
            ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver
    ) {
        super.setResourceTypeAndIdResolverInternal(resourceTypeAndIdResolver);
        return new MultipleResourcesTerminalStage<>(
                getRequest(),
                dataSupplier,
                processorContext,
                MultipleResourcesJsonApiContext.<REQUEST, DATA_SOURCE_DTO, Object>builder()
                        .attributesResolver(dto -> null)
                        .resourceTypeAndIdResolver(getResourceTypeAndIdResolver())
                        .defaultRelationshipResolvers(getDefaultRelationshipResolvers())
                        .toManyRelationshipResolvers(getToManyRelationshipResolvers())
                        .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers())
                        .toOneRelationshipResolvers(getToOneRelationshipResolvers())
                        .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers())
                        .dataResolutionIsConfiguredFor(getRelationshipTypes())
                        .topLevelLinksResolver(topLevelLinksResolver)
                        .resourceLinksResolver(getResourceLinksResolver())
                        .topLevelMetaResolver(topLevelMetaResolver)
                        .resourceMetaResolver(getResourceMetaResolver())
                        .build()
        );
    }

}
