package pro.api4.jsonapi4j.processor.single.resource;

import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.ResourceSupplier;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlRequerementsEvaluator;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForResource;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForResource.ResourceAnonymizationResult;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.util.Map;
import java.util.stream.Stream;

public class SingleResourceTerminalStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final REQUEST request;
    private final SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final ResourceProcessorContext processorContext;
    private final SingleResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiMembersResolver;

    SingleResourceTerminalStage(REQUEST request,
                                SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                ResourceProcessorContext processorContext,
                                SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiMembersResolver = new SingleResourceJsonApiMembersResolver<>(
                jsonApiContext,
                processorContext.getExecutor()
        );
    }

    /**
     * Triggers the terminal operation that generates the final JSON:API Document based on all previously configured
     * settings. Doesn't require any relationship-related configurations and as a result doesn't require
     * <code>relationshipsSupplier</code> parameter. Can be used for those JSON:API resources that don't have any
     * relationships.
     *
     * @param resourceSupplier function that creates a new instance of a primary resource by passing attributes object,
     *                         relationships object (usually, <code>null</code>), resource-level
     *                         <code>JsonApiLinks</code>, id, type, and resource-level meta
     * @param responseSupplier function that creates a new instance of a single-resource JSON:API Document by passing
     *                         the resolved instance of a primary resource, doc-level meta, and doc-level links
     * @param <RESOURCE>       type of the primary resource, must extend {@link ResourceObject}
     * @param <DOC>            type of the final single-primary-resource JSON:API Document, must extend
     *                         {@link SingleResourceDoc}
     * @return Single-primary-resource JSON:API Document (that doesn't have any relationships)
     */
    public <RESOURCE extends ResourceObject<ATTRIBUTES, Object>,
            DOC extends SingleResourceDoc<RESOURCE>> DOC toSingleResourceDoc(
            ResourceSupplier<ATTRIBUTES, Object, RESOURCE> resourceSupplier,
            SingleResourceDocSupplier<RESOURCE, DOC> responseSupplier
    ) {
        return toSingleResourceDoc(null, resourceSupplier, responseSupplier);
    }

    public SingleResourceDoc<?> toSingleResourceDoc() {
        return toSingleResourceDoc(
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
                (SingleResourceDocSupplier<ResourceObject<ATTRIBUTES, Map<String, Object>>, SingleResourceDoc<ResourceObject<ATTRIBUTES, Map<String, Object>>>>) SingleResourceDoc::new
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
     * @param <DOC>                 type of the final single-primary-resource JSON:API Document, must extend
     *                              {@link SingleResourceDoc}
     * @return Single-primary-resource JSON:API Document
     */
    public <RELATIONSHIPS,
            RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>,
            DOC extends SingleResourceDoc<RESOURCE>> DOC toSingleResourceDoc(
            RelationshipsSupplier<RELATIONSHIPS> relationshipsSupplier,
            ResourceSupplier<ATTRIBUTES, RELATIONSHIPS, RESOURCE> resourceSupplier,
            SingleResourceDocSupplier<RESOURCE, DOC> docSupplier
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
        DATA_SOURCE_DTO dataSourceDto = inboundAcEvaluator.retrieveDataAndEvaluateInboundAcReq(
                request,
                () -> DataRetrievalUtil.retrieveDataLenient(() -> dataSupplier.get(request))
        );

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, dataSourceDto);
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, dataSourceDto);

        // return if downstream response is null or inbound access is not allowed
        if (dataSourceDto == null) {
            return docSupplier.get(null, docLinks, docMeta);
        }

        // resource id and type
        IdAndType idAndType = jsonApiMembersResolver.resolveResourceIdAndType(dataSourceDto);
        // attributes
        ATTRIBUTES att = jsonApiMembersResolver.resolveAttributes(dataSourceDto);
        // resource links
        LinksObject resourceLinks = jsonApiMembersResolver.resolveResourceLinks(request, dataSourceDto);
        // resource meta
        Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(request, dataSourceDto);

        // compose resource without relationships
        RESOURCE resource = resourceSupplier.get(
                idAndType.getId(),
                idAndType.getType().getType(),
                att,
                null,
                resourceLinks,
                resourceMeta
        );

        if (resource == null) {
            return docSupplier.get(null, docLinks, docMeta);
        }

        //
        // Outbound Access Control checks + relationships resolution
        //
        OutboundAccessControlRequirementsEvaluatorForResource outboundAcEvaluator = new OutboundAccessControlRequirementsEvaluatorForResource(
                processorContext.getAccessControlEvaluator(),
                processorContext.getOutboundAccessControlSettings()
        );

        // apply settings from annotations if any
        outboundAcEvaluator.calculateEffectiveAccessControlSettings(
                resource.getClass(),
                att != null ? att.getClass() : null
        );

        // anonymize resource if needed
        ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, RELATIONSHIPS> resourceAnonymizationResult
                = outboundAcEvaluator.anonymizeResourceIfNeeded(resource);

        if (resourceAnonymizationResult.isFullyAnonymized()) {
            return docSupplier.get(null, docLinks, docMeta);
        }

        // resolve and anonymize resource relationships if needed
        outboundAcEvaluator.resolveAndAnonymizeRelationshipsIfNeeded(
                resourceAnonymizationResult,
                () -> jsonApiMembersResolver.resolveResourceRelationshipsInParallel(
                        request,
                        dataSourceDto,
                        relationshipsSupplier
                )
        );

        // anonymize resource attributes if needed
        outboundAcEvaluator.anonymizeAttributesIfNeeded(resourceAnonymizationResult);

        return docSupplier.get(resource, docLinks, docMeta);
    }

}
