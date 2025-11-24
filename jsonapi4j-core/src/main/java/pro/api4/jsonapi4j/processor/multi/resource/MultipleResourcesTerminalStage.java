package pro.api4.jsonapi4j.processor.multi.resource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.ac.AnonymizationResult;
import pro.api4.jsonapi4j.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.ResourceSupplier;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsRetrievalStage;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.ProcessingItem;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static pro.api4.jsonapi4j.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.ATTRIBUTES_FIELD;
import static pro.api4.jsonapi4j.processor.util.AccessControlUtil.getEffectiveOutboundAccessControlSettings;


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
                            ).sorted(Comparator.comparing(e -> e.getKey().getName()))
                            .collect(CustomCollectors.toOrderedMapThatSupportsNullValues(
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

        AccessControlEvaluator accessControlEvaluator
                = processorContext.getAccessControlEvaluator();

        CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse = new MultipleDataItemsRetrievalStage(
                accessControlEvaluator,
                processorContext.getInboundAccessControlSettings()
        ).retrieveData(request, dataSupplier);

        // return if downstream response is null or inbound access is not allowed
        if (cursorPageableResponse == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, null, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, null);
            // compose doc
            return docSupplier.get(Collections.emptyList(), docLinks, docMeta);
        }

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(
                request,
                cursorPageableResponse.getItems(),
                cursorPageableResponse.getNextCursor()
        );
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, cursorPageableResponse.getItems());

        // build raw resources without relationships
        List<ProcessingItem<DATA_SOURCE_DTO, RESOURCE>> processingItems = emptyIfNull(cursorPageableResponse.getItems())
                .stream()
                .map(dto -> {
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
                    return new ProcessingItem<>(dto, resource);
                }).toList();

        processingItems = processingItems.stream()
                .filter(processingItem -> processingItem.getResource() != null)
                .collect(Collectors.toList());

        if (processingItems.isEmpty()) {
            return docSupplier.get(Collections.emptyList(), docLinks, docMeta);
        }

        OutboundAccessControlForCustomClass outboundAccessControlSettings = getEffectiveOutboundAccessControlSettings(
                processingItems,
                processorContext.getOutboundAccessControlSettings()
        );

        processingItems = processingItems
                .stream()
                .peek(processingItem -> {
                    RESOURCE resource = processingItem.getResource();
                    AnonymizationResult<RESOURCE> resourceAnonymizationResult = anonymizeObjectIfNeeded(
                            accessControlEvaluator,
                            resource,
                            resource,
                            outboundAccessControlSettings
                    );
                    processingItem.setAnonymizationResult(resourceAnonymizationResult);
                })
                .filter(processingItem -> !processingItem.getAnonymizationResult().isFullyAnonymized())
                .toList();

        // relationships resolution + anonymization
        Map<DATA_SOURCE_DTO, RELATIONSHIPS> relationshipsMap = jsonApiMembersResolver.resolveResourceRelationshipsInParallel(
                request,
                processingItems.stream()
                        .map(ProcessingItem::getResourceDto)
                        .toList(),
                relationshipsSupplier
        );

        // set relationships to resources
        processingItems.forEach(processingItem -> {
            DATA_SOURCE_DTO resourceDto = processingItem.getResourceDto();
            if (relationshipsMap.containsKey(resourceDto)) {
                processingItem.getAnonymizationResult().targetObject().setRelationships(relationshipsMap.get(resourceDto));
            }
        });

        // compose 'data'
        List<RESOURCE> data = processingItems
                .stream()
                .map(ProcessingItem::getAnonymizationResult)
                .map(AnonymizationResult::targetObject)
                .toList();

        // compose doc
        return docSupplier.get(data, docLinks, docMeta);
    }

}
