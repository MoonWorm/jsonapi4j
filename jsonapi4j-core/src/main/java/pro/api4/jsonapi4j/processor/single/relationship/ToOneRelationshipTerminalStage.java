package pro.api4.jsonapi4j.processor.single.relationship;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors.DataPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors.DataPreRetrievalPhase;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.List;

@Slf4j
public class ToOneRelationshipTerminalStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;
    private final SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final RelationshipProcessorContext processorContext;
    private final ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;
    private final ToOneRelationshipJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> jsonApiMembersResolver;

    ToOneRelationshipTerminalStage(REQUEST request,
                                   SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                   RelationshipProcessorContext processorContext,
                                   ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiContext = jsonApiContext;
        this.jsonApiMembersResolver = new ToOneRelationshipJsonApiMembersResolver<>(
                jsonApiContext
        );
    }

    public <DOC extends ToOneRelationshipDoc> DOC toToOneRelationshipDoc(
            ToOneRelationshipDocSupplier<DOC> docSupplier
    ) {
        // validation
        Validate.notNull(docSupplier);

        REQUEST effectiveRequest = this.request;
        List<PluginSettings> plugins = this.processorContext.getPlugins();

        // PHASE: onDataPreRetrieval
        for (PluginSettings plugin : plugins) {
            ToOneRelationshipVisitors visitors = plugin.getPlugin().toOneRelationshipVisitors();
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

        DATA_SOURCE_DTO dataSourceDto = retrieveData(effectiveRequest);

        // return if downstream response is null or inbound access is not allowed
        if (dataSourceDto == null) {
            // doc links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(effectiveRequest, null);
            // doc meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, null);
            return docSupplier.get(null, docLinks, docMeta);
        }

        // resource id and type
        IdAndType idAndType = jsonApiMembersResolver.resolveResourceTypeAndId(dataSourceDto);
        // resource meta
        Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(effectiveRequest, dataSourceDto);
        // compose data
        ResourceIdentifierObject data = new ResourceIdentifierObject(
                idAndType.getId(),
                idAndType.getType().getType(),
                resourceMeta
        );

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(effectiveRequest, dataSourceDto);

        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(effectiveRequest, dataSourceDto);

        // compose response
        DOC doc = docSupplier.get(data, docLinks, docMeta);

        // PHASE: onDataPostRetrieval
        for (PluginSettings plugin : plugins) {
            ToOneRelationshipVisitors visitors = plugin.getPlugin().toOneRelationshipVisitors();
            if (visitors != null) {
                DataPostRetrievalPhase<?> dataPostRetrievalPhase = visitors.onDataPostRetrieval(
                        effectiveRequest,
                        dataSourceDto,
                        doc,
                        jsonApiContext,
                        plugin.getInfo()
                );
                if (dataPostRetrievalPhase.getContinuation() == DataPostRetrievalPhase.Continuation.MUTATE_DOC) {
                    //noinspection unchecked
                    doc = ((DataPostRetrievalPhase<DOC>) dataPostRetrievalPhase).getResult();
                } else if (dataPostRetrievalPhase.getContinuation() == DataPostRetrievalPhase.Continuation.RETURN_DOC) {
                    //noinspection unchecked
                    return ((DataPostRetrievalPhase<DOC>) dataPostRetrievalPhase).getResult();
                }
            }
        }

        return doc;
    }

    public ToOneRelationshipDoc toToOneRelationshipDoc() {
        return toToOneRelationshipDoc(ToOneRelationshipDoc::new);
    }

    private DATA_SOURCE_DTO retrieveData(REQUEST req) {
        return DataRetrievalUtil.retrieveDataNullable(() -> dataSupplier.get(req));
    }

}
