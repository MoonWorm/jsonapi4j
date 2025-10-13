package io.jsonapi4j.processor.multi.relationship;

import io.jsonapi4j.processor.RelationshipProcessorContext;
import io.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import io.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import io.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import io.jsonapi4j.model.document.LinksObject;

public class ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;
    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final RelationshipProcessorContext processorContext;
    private final ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    public ToManyRelationshipsJsonApiConfigurationStage(REQUEST request,
                                                        MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                                        RelationshipProcessorContext processorContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiContext = new ToManyRelationshipsJsonApiContext<>();
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
    public ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver(
            MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver
    ) {
        this.jsonApiContext.setTopLevelLinksResolver(topLevelLinksResolver);
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
    public ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver(
            MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver
    ) {
        this.jsonApiContext.setTopLevelMetaResolver(topLevelMetaResolver);
        return this;
    }

    /**
     * Configures resource-identifier-level 'meta' object.
     * <p/>
     * Optional. Use if you want to populate a resource-level 'meta' object.
     *
     * @param resourceMetaResolver function that generates free-form 'meta' Object by accepting the original request
     *                             object and data source dto object
     * @return self link to the terminal operation allowing to apply other configurations
     */
    public ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> resourceIdentifierMetaResolver(
            ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver
    ) {
        this.jsonApiContext.setResourceMetaResolver(resourceMetaResolver);
        return this;
    }

    /**
     * Sets resource identifier type and id resolver.
     *
     * @param resourceTypeAndIdResolver - function that retrieves unique resource id and type from {@link DATA_SOURCE_DTO}
     * @return self ref
     */
    public ToManyRelationshipsTerminalStage<REQUEST, DATA_SOURCE_DTO> resourceTypeAndIdSupplier(
            ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver
    ) {

        this.jsonApiContext.setResourceTypeAndIdResolver(resourceTypeAndIdResolver);
        return new ToManyRelationshipsTerminalStage<>(
                request,
                dataSupplier,
                processorContext,
                jsonApiContext
        );
    }

}
