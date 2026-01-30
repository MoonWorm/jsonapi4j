package pro.api4.jsonapi4j.processor.multi.relationship;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors.DataPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors.DataPreRetrievalPhase;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ToManyRelationshipsTerminalStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;
    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final RelationshipProcessorContext processorContext;
    private final ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;
    private final ToManyRelationshipsJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> jsonApiMembersResolver;

    ToManyRelationshipsTerminalStage(REQUEST request,
                                     MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                     RelationshipProcessorContext processorContext,
                                     ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiContext = jsonApiContext;
        this.jsonApiMembersResolver = new ToManyRelationshipsJsonApiMembersResolver<>(
                jsonApiContext
        );
    }

    public <DOC extends ToManyRelationshipsDoc> DOC toToManyRelationshipsDoc(
            ToManyRelationshipsDocSupplier<DOC> docSupplier
    ) {
        // validation
        Validate.notNull(docSupplier);

        REQUEST effectiveRequest = this.request;

        List<PluginSettings> plugins = this.processorContext.getPlugins();

        // PHASE: onDataPreRetrieval
        for (PluginSettings plugin : plugins) {
            ToManyRelationshipVisitors visitors = plugin.getPlugin().toManyRelationshipVisitors();
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

        CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse = retrieveData(request);

        // return if downstream response is null or inbound access is not allowed or the response is null
        if (cursorPageableResponse == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, null, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, null);
            // compose doc
            return docSupplier.get(Collections.emptyList(), docLinks, docMeta);
        }

        List<ResourceIdentifierObject> data = null;
        if (cursorPageableResponse.getItems() != null) {
            data = cursorPageableResponse.getItems().stream()
                    .map(dto -> {
                        // id and type
                        IdAndType idAndType = jsonApiMembersResolver.resolveResourceTypeAndId(dto);
                        // resource identifier meta
                        Object resourceIdentifierMeta = jsonApiMembersResolver.resolveResourceMeta(request, dto);
                        // compose resource identifier
                        return new ResourceIdentifierObject(
                                idAndType.getId(),
                                idAndType.getType().getType(),
                                resourceIdentifierMeta
                        );
                    }).toList();
        }

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(
                request,
                cursorPageableResponse.getItems(),
                cursorPageableResponse.getNextCursor()
        );
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, cursorPageableResponse.getItems());

        DOC doc = docSupplier.get(data, docLinks, docMeta);

        // PHASE: onDataPostRetrieval
        for (PluginSettings plugin : plugins) {
            ToManyRelationshipVisitors visitors = plugin.getPlugin().toManyRelationshipVisitors();
            if (visitors != null) {
                DataPostRetrievalPhase<?> dataPostRetrievalPhase = visitors.onDataPostRetrieval(
                        request,
                        cursorPageableResponse,
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

    public ToManyRelationshipsDoc toToManyRelationshipsDoc() {
        return toToManyRelationshipsDoc(ToManyRelationshipsDoc::new);
    }

    private CursorPageableResponse<DATA_SOURCE_DTO> retrieveData(REQUEST req) {
        return DataRetrievalUtil.retrieveDataNullable(() -> dataSupplier.get(req));
    }

}
