package pro.api4.jsonapi4j.processor.single.resource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.*;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors.DataPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors.DataPreRetrievalPhase;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors.RelationshipsPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors.RelationshipsPreRetrievalPhase;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.processor.*;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SingleResourceTerminalStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final REQUEST request;
    private final SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final ResourceProcessorContext processorContext;
    private final SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;
    private final SingleResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiMembersResolver;

    SingleResourceTerminalStage(REQUEST request,
                                SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                ResourceProcessorContext processorContext,
                                SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiContext = jsonApiContext;
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
                            ).sorted(Comparator.comparing(Map.Entry::getKey))
                            .collect(CustomCollectors.toOrderedMapThatSupportsNullValues(
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

        REQUEST effectiveRequest = this.request;
        List<PluginSettings> plugins = this.processorContext.getPlugins();

        // PHASE: onDataPreRetrieval
        for (PluginSettings plugin : plugins) {
            SingleResourceVisitors visitors = plugin.getPlugin().singleResourceVisitors();
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

        // RETRIEVE DATA
        DATA_SOURCE_DTO dataSourceDto = retrieveData(effectiveRequest);

        // PHASE: onDataPostRetrieval
        for (PluginSettings plugin : plugins) {
            SingleResourceVisitors visitors = plugin.getPlugin().singleResourceVisitors();
            if (visitors != null) {
                DataPostRetrievalPhase<?> dataPostRetrievalPhase = visitors.onDataPostRetrieval(
                        effectiveRequest,
                        dataSourceDto,
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

        if (dataSourceDto == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(effectiveRequest, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, null);

            return docSupplier.get(null, docLinks, docMeta);
        }

        // resource id and type
        IdAndType idAndType = jsonApiMembersResolver.resolveResourceIdAndType(dataSourceDto);
        // attributes
        ATTRIBUTES att = jsonApiMembersResolver.resolveAttributes(dataSourceDto);
        // resource links
        LinksObject resourceLinks = jsonApiMembersResolver.resolveResourceLinks(effectiveRequest, dataSourceDto);
        // resource meta
        Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(effectiveRequest, dataSourceDto);

        // compose resource without relationships
        RESOURCE data = resourceSupplier.get(
                idAndType.getId(),
                idAndType.getType().getType(),
                att,
                null,
                resourceLinks,
                resourceMeta
        );

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(effectiveRequest, dataSourceDto);
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, dataSourceDto);

        // compose doc
        DOC doc = docSupplier.get(data, docLinks, docMeta);

        // PHASE: onRelationshipsPreRetrieval
        for (PluginSettings plugin : plugins) {
            SingleResourceVisitors visitors = plugin.getPlugin().singleResourceVisitors();
            if (visitors != null) {
                RelationshipsPreRetrievalPhase<?> relationshipsPreRetrievalPhase = visitors.onRelationshipsPreRetrieval(
                        effectiveRequest,
                        dataSourceDto,
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

        data = doc.getData();

        if (data == null) {
            return docSupplier.get(null, docLinks, docMeta);
        }

        // resolve relationships
        RELATIONSHIPS relationships = jsonApiMembersResolver.resolveResourceRelationshipsInParallel(
                effectiveRequest,
                dataSourceDto,
                relationshipsSupplier
        );

        // set relationships
        ReflectionUtils.setFieldValue(doc.getData(), ResourceObject.RELATIONSHIPS_FIELD, relationships);

        // PHASE: onRelationshipsPostRetrieval
        for (PluginSettings plugin : plugins) {
            SingleResourceVisitors visitors = plugin.getPlugin().singleResourceVisitors();
            if (visitors != null) {
                RelationshipsPostRetrievalPhase<?> relationshipsPostRetrievalPhase = visitors.onRelationshipsPostRetrieval(
                        effectiveRequest,
                        dataSourceDto,
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

        // return doc
        return doc;
    }

    private DATA_SOURCE_DTO retrieveData(REQUEST req) {
        return DataRetrievalUtil.retrieveDataNullable(() -> dataSupplier.get(req));
    }

}
