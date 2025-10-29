package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlRequerementsEvaluator;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForRelationship;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ToManyRelationshipsTerminalStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;
    private final MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final RelationshipProcessorContext processorContext;
    private final ToManyRelationshipsJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> jsonApiMembersResolver;

    ToManyRelationshipsTerminalStage(REQUEST request,
                                     MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                     RelationshipProcessorContext processorContext,
                                     ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiMembersResolver = new ToManyRelationshipsJsonApiMembersResolver<>(
                jsonApiContext
        );
    }

    public <DOC extends ToManyRelationshipsDoc> DOC toToManyRelationshipsDoc(
            ToManyRelationshipsDocSupplier<DOC> docSupplier
    ) {
        // validation
        Validate.notNull(docSupplier);

        //
        // Inbound Access Control checks + retrieve cursorPageableResponse
        //
        InboundAccessControlRequerementsEvaluator inboundAcEvaluator = new InboundAccessControlRequerementsEvaluator(
                processorContext.getAccessControlEvaluator(),
                processorContext.getInboundAccessControlSettings()
        );
        CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse = inboundAcEvaluator.retrieveDataAndEvaluateInboundAcReq(
                request,
                () -> DataRetrievalUtil.retrieveDataLenient(() -> dataSupplier.get(request))
        );

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

        //
        // Outbound Access Control checks + doc composing
        //
        OutboundAccessControlRequirementsEvaluatorForRelationship outboundAcEvaluator = new OutboundAccessControlRequirementsEvaluatorForRelationship(
                processorContext.getAccessControlEvaluator(),
                processorContext.getOutboundAccessControlSettings()
        );

        List<ResourceIdentifierObject> data = cursorPageableResponse
                .getItems()
                .stream()
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
                })
                .map(outboundAcEvaluator::anonymizeResourceIdentifierIfNeeded)
                .filter(Objects::nonNull)
                .toList();

        // compose response
        return docSupplier.get(data, docLinks, docMeta);
    }

    public ToManyRelationshipsDoc toToManyRelationshipsDoc() {
        return toToManyRelationshipsDoc(ToManyRelationshipsDoc::new);
    }

}
