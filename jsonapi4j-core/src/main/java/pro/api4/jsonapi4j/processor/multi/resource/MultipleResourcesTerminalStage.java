package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.ResourceSupplier;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlRequerementsEvaluator;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForResource;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForResource.ResourceAnonymizationResult;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class MultipleResourcesTerminalStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final REQUEST request;
    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final ResourceProcessorContext processorContext;
    private final MultipleResourcesJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiMembersResolver;

    public MultipleResourcesTerminalStage(REQUEST request,
                                          MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                          ResourceProcessorContext processorContext,
                                          MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiMembersResolver = new MultipleResourcesJsonApiMembersResolver<>(
                jsonApiContext,
                processorContext.getExecutor()
        );
    }

    public MultipleResourcesDoc<?> toMultipleResourcesDoc() {
        return toMultipleResourcesDoc(
                (toManyRelationshipsDocMap, toOneRelationshipDocMap) -> {
                    if (MapUtils.isEmpty(toManyRelationshipsDocMap) && MapUtils.isEmpty(toOneRelationshipDocMap)) {
                        return null;
                    }
                    return Stream.concat(
                            toManyRelationshipsDocMap.entrySet().stream(),
                            toOneRelationshipDocMap.entrySet().stream()
                    ).collect(CustomCollectors.toMapThatSupportsNullValues(
                            e -> e.getKey().getName(),
                            Map.Entry::getValue
                    ));
                },
                (ResourceSupplier<ATTRIBUTES, Map<String, Object>, ResourceObject<ATTRIBUTES, Map<String, Object>>>) ResourceObject::new,
                (MultipleResourcesDocSupplier<ResourceObject<ATTRIBUTES, Map<String, Object>>, MultipleResourcesDoc<ResourceObject<ATTRIBUTES, Map<String, Object>>>>) MultipleResourcesDoc::new
        );
    }

    /**
     * Triggers the terminal operation that generates the final JSON:API Document based on all previously configured
     * settings.
     *
     * @param relationshipsSupplier function that creates a new instance of 'relationships' object by passing all the
     *                              resolved To Many relationships
     *                              - {@link ToManyRelationshipsDoc},
     *                              and all the resolved to-one relationships
     *                              - {@link ToOneRelationshipDoc}
     * @param resourceSupplier      function that creates a new instance of a primary resource by passing attributes object,
     *                              relationships object (see the <code>relationshipsSupplier</code> parameter), resource-level
     *                              <code>JsonApiLinks</code>, id, type, and resource-level meta
     * @param docSupplier           function that creates a new instance of a single-resource JSON:API Document by passing
     *                              the resolved instance of a primary resource, doc-level meta, and doc-level links
     * @param <RELATIONSHIPS>       type of resource related 'relationships' object
     * @param <RESOURCE>            type of the primary resource, must extend {@link ResourceObject}
     * @param <DOC>                 type of the final multi-primary-resource JSON:API Document, must extend
     *                              {@link MultipleResourcesDoc}
     * @return Multi-primary-resource JSON:API Document
     */
    public <RELATIONSHIPS,
            RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>,
            DOC extends MultipleResourcesDoc<RESOURCE>> DOC toMultipleResourcesDoc(
            RelationshipsSupplier<RELATIONSHIPS> relationshipsSupplier,
            ResourceSupplier<ATTRIBUTES, RELATIONSHIPS, RESOURCE> resourceSupplier,
            MultipleResourcesDocSupplier<RESOURCE, DOC> docSupplier
    ) {

        // validations
        Validate.notNull(resourceSupplier);
        Validate.notNull(docSupplier);

        //
        // Inbound Access Control checks + retrieve data source dto
        //
        InboundAccessControlRequerementsEvaluator inboundAcEvaluator = new InboundAccessControlRequerementsEvaluator(
                processorContext.getAccessControlEvaluator(),
                processorContext.getInboundAccessControlSettings()
        );
        CursorPageableResponse<DATA_SOURCE_DTO> downstreamResponse = inboundAcEvaluator.retrieveDataAndEvaluateInboundAcReq(
                request,
                () -> DataRetrievalUtil.retrieveDataLenient(() -> dataSupplier.get(request))
        );

        // return if downstream response is null or inbound access is not allowed
        if (downstreamResponse == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, null, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, null);
            // compose doc
            return docSupplier.get(null, docLinks, docMeta);
        }

        OutboundAccessControlRequirementsEvaluatorForResource outboundAcEvaluator = new OutboundAccessControlRequirementsEvaluatorForResource(
                processorContext.getAccessControlEvaluator(),
                processorContext.getOutboundAccessControlSettings()
        );

        boolean isEffectiveAccessControlSettingsCalculated = false;

        List<IntermediateResultItem<DATA_SOURCE_DTO, ATTRIBUTES, RELATIONSHIPS, RESOURCE>> intermediateResult
                = new ArrayList<>();

        for (DATA_SOURCE_DTO dto : downstreamResponse.getItems()) {
            // resource id and type
            IdAndType idAndType = jsonApiMembersResolver.resolveResourceIdAndType(dto);
            // attributes
            ATTRIBUTES att = jsonApiMembersResolver.resolveAttributes(dto);
            // resource links
            LinksObject resourceLinks = jsonApiMembersResolver.resolveResourceLinks(request, dto);
            // resource meta
            Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(request, dto);
            // resource without relationships
            RESOURCE resource = resourceSupplier.get(
                    idAndType.getId(),
                    idAndType.getType().getType(),
                    att,
                    null,
                    resourceLinks,
                    resourceMeta
            );

            // filter out 'null' values below
            if (resource == null) {
                return null;
            }

            // apply settings from annotations if any but just once
            if (!isEffectiveAccessControlSettingsCalculated) {
                outboundAcEvaluator.calculateEffectiveAccessControlSettings(
                        resource.getClass(),
                        att != null ? att.getClass() : null
                );
                isEffectiveAccessControlSettingsCalculated = true;
            }

            // anonymize resource if needed
            ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, RELATIONSHIPS> resourceAnonymizationResult
                    = outboundAcEvaluator.anonymizeResourceIfNeeded(resource);

            intermediateResult.add(new IntermediateResultItem<>(dto, resourceAnonymizationResult));
        }

        // relationships resolution + anonymization
        outboundAcEvaluator.bulkResolveAndAnonymizeRelationshipsIfNeeded(
                intermediateResult,
                () -> jsonApiMembersResolver.resolveResourceRelationshipsInParallel(
                        request,
                        intermediateResult.stream().map(IntermediateResultItem::dataSourceDto).toList(),
                        relationshipsSupplier
                )
        );

        // attributes anonymization
        List<RESOURCE> data = intermediateResult
                .stream()
                .map(resultItem -> {
                    ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, RELATIONSHIPS> resourceAnonymizationResult
                            = resultItem.resourceAnonymizationResult();

                    outboundAcEvaluator.anonymizeAttributesIfNeeded(resourceAnonymizationResult);

                    return resourceAnonymizationResult.resource();

                }).toList();

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(
                request,
                downstreamResponse.getItems(),
                downstreamResponse.getNextCursor()
        );
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, downstreamResponse.getItems());
        // compose doc
        return docSupplier.get(data, docLinks, docMeta);
    }

    public record IntermediateResultItem<DATA_SOURCE_DTO, ATTRIBUTES, RELATIONSHIPS, RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>>(
            DATA_SOURCE_DTO dataSourceDto,
            ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, RELATIONSHIPS> resourceAnonymizationResult
    ) {
    }

}
