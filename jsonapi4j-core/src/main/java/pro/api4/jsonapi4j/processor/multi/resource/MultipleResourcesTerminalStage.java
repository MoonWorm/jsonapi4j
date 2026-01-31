package pro.api4.jsonapi4j.processor.multi.resource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors.DataPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors.DataPreRetrievalPhase;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors.RelationshipsPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors.RelationshipsPreRetrievalPhase;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.processor.*;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;


public class MultipleResourcesTerminalStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final REQUEST request;
    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final ResourceProcessorContext processorContext;
    private final MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;
    private final MultipleResourcesJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiMembersResolver;

    public MultipleResourcesTerminalStage(REQUEST request,
                                          MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                          ResourceProcessorContext processorContext,
                                          MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiContext = jsonApiContext;
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
                            ).sorted(Comparator.comparing(Map.Entry::getKey))
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

        REQUEST effectiveRequest = this.request;

        List<PluginSettings> plugins = this.processorContext.getPlugins();

        // PHASE: onDataPreRetrieval
        for (PluginSettings plugin : plugins) {
            MultipleResourcesVisitors visitors = plugin.getPlugin().multipleResourcesVisitors();
            if (visitors != null) {
                DataPreRetrievalPhase<?> dataPreRetrievalPhase = visitors.onDataPreRetrieval(
                        effectiveRequest,
                        jsonApiContext,
                        plugin.getInfo()
                );
                if (dataPreRetrievalPhase.getContinuation() == DataPreRetrievalPhase.Continuation.MUTATE_REQUEST) {
                    //noinspection unchecked
                    effectiveRequest = ((DataPreRetrievalPhase<REQUEST>) dataPreRetrievalPhase).getResult();
                } else if (dataPreRetrievalPhase.getContinuation() == DataPreRetrievalPhase.Continuation.RETURN_DOC) {
                    //noinspection unchecked
                    return ((DataPreRetrievalPhase<DOC>) dataPreRetrievalPhase).getResult();
                }
            }
        }

        CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse = retrieveData(effectiveRequest);

        // PHASE: onDataPostRetrieval
        for (PluginSettings plugin : plugins) {
            MultipleResourcesVisitors visitors = plugin.getPlugin().multipleResourcesVisitors();
            if (visitors != null) {
                DataPostRetrievalPhase<?> dataPostRetrievalPhase = visitors.onDataPostRetrieval(
                        effectiveRequest,
                        cursorPageableResponse,
                        jsonApiContext,
                        plugin.getInfo()
                );
                if (dataPostRetrievalPhase.getContinuation() == DataPostRetrievalPhase.Continuation.MUTATE_REQUEST) {
                    //noinspection unchecked
                    effectiveRequest = ((DataPostRetrievalPhase<REQUEST>) dataPostRetrievalPhase).getResult();
                } else if (dataPostRetrievalPhase.getContinuation() == DataPostRetrievalPhase.Continuation.RETURN_DOC) {
                    //noinspection unchecked
                    return ((DataPostRetrievalPhase<DOC>) dataPostRetrievalPhase).getResult();
                }
            }
        }

        if (cursorPageableResponse == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(effectiveRequest, null, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, null);
            // compose doc
            return docSupplier.get(Collections.emptyList(), docLinks, docMeta);
        }

        // compose data
        List<RESOURCE> data = null;
        if (cursorPageableResponse.getItems() != null) {
            data = new ArrayList<>();
            for (DATA_SOURCE_DTO dto : cursorPageableResponse.getItems()) {
                // resource id and type
                IdAndType idAndType = jsonApiMembersResolver.resolveResourceIdAndType(dto);
                // attributes
                ATTRIBUTES att = jsonApiMembersResolver.resolveAttributes(dto);
                // resource links
                LinksObject resourceLinks = jsonApiMembersResolver.resolveResourceLinks(effectiveRequest, dto);
                // resource meta
                Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(effectiveRequest, dto);
                // resource without relationships
                RESOURCE resource = resourceSupplier.get(
                        idAndType.getId(),
                        idAndType.getType().getType(),
                        att,
                        null,
                        resourceLinks,
                        resourceMeta
                );
                data.add(resource);
            }
        }
        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(
                effectiveRequest,
                cursorPageableResponse.getItems(),
                cursorPageableResponse.getNextCursor()
        );
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, cursorPageableResponse.getItems());

        // compose doc
        DOC doc = docSupplier.get(data, docLinks, docMeta);

        // PHASE: onRelationshipsPreRetrieval
        for (PluginSettings plugin : plugins) {
            MultipleResourcesVisitors visitors = plugin.getPlugin().multipleResourcesVisitors();
            if (visitors != null) {
                RelationshipsPreRetrievalPhase<?> relationshipsPreRetrievalPhase = visitors.onRelationshipsPreRetrieval(
                        effectiveRequest,
                        cursorPageableResponse,
                        doc,
                        jsonApiContext,
                        plugin.getInfo()
                );
                if (relationshipsPreRetrievalPhase.getContinuation() == RelationshipsPreRetrievalPhase.Continuation.MUTATE_DOC) {
                    //noinspection unchecked
                    doc = ((RelationshipsPreRetrievalPhase<DOC>) relationshipsPreRetrievalPhase).getResult();
                } else if (relationshipsPreRetrievalPhase.getContinuation() == RelationshipsPreRetrievalPhase.Continuation.RETURN_DOC) {
                    //noinspection unchecked
                    return ((RelationshipsPreRetrievalPhase<DOC>) relationshipsPreRetrievalPhase).getResult();
                }
            }
        }

        // filter out dtos
        Map<IdAndType, DATA_SOURCE_DTO> idAndTypeToDtoMap = emptyIfNull(cursorPageableResponse.getItems()).stream()
                .collect(Collectors.toMap(
                        jsonApiMembersResolver::resolveResourceIdAndType,
                        dto -> dto
                ));
        List<DATA_SOURCE_DTO> effectiveDataSourceDtos = emptyIfNull(doc.getData()).stream()
                .map(resource -> idAndTypeToDtoMap.get(
                        new IdAndType(
                                resource.getId(),
                                new ResourceType(resource.getType())
                        )
                )).filter(Objects::nonNull)
                .toList();

        // relationships resolution
        Map<IdAndType, RELATIONSHIPS> relationshipsMap = jsonApiMembersResolver.resolveResourceRelationshipsInParallel(
                effectiveRequest,
                effectiveDataSourceDtos,
                relationshipsSupplier
        );

        // set relationships to resources
        Map<IdAndType, ResourceObject<?, ?>> idAndTypeToResourceMap = emptyIfNull(doc.getData()).stream()
                .collect(Collectors.toMap(
                        resource -> new IdAndType(resource.getId(), new ResourceType(resource.getType())),
                        resource -> resource
                ));
        relationshipsMap.forEach((idAndType, relationships) -> {
            ResourceObject<?, ?> relatedResource = idAndTypeToResourceMap.get(idAndType);
            if (relatedResource != null) {
                ReflectionUtils.setFieldValue(
                        relatedResource,
                        ResourceObject.RELATIONSHIPS_FIELD,
                        relationships
                );
            }
        });

        // PHASE: onRelationshipsPostRetrieval
        for (PluginSettings plugin : plugins) {
            MultipleResourcesVisitors visitors = plugin.getPlugin().multipleResourcesVisitors();
            if (visitors != null) {
                RelationshipsPostRetrievalPhase<?> relationshipsPostRetrievalPhase = visitors.onRelationshipsPostRetrieval(
                        effectiveRequest,
                        cursorPageableResponse,
                        doc,
                        jsonApiContext,
                        plugin.getInfo()
                );
                if (relationshipsPostRetrievalPhase.getContinuation() == RelationshipsPostRetrievalPhase.Continuation.MUTATE_DOC) {
                    //noinspection unchecked
                    doc = ((RelationshipsPostRetrievalPhase<DOC>) relationshipsPostRetrievalPhase).getResult();
                } else if (relationshipsPostRetrievalPhase.getContinuation() == RelationshipsPostRetrievalPhase.Continuation.RETURN_DOC) {
                    //noinspection unchecked
                    return ((RelationshipsPostRetrievalPhase<DOC>) relationshipsPostRetrievalPhase).getResult();
                }
            }
        }

        // compose doc
        return docSupplier.get(data, docLinks, docMeta);
    }

    private CursorPageableResponse<DATA_SOURCE_DTO> retrieveData(REQUEST req) {
        return DataRetrievalUtil.retrieveDataNullable(() -> dataSupplier.get(req));
    }

}
