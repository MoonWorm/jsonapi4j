package pro.api4.jsonapi4j.processor.multi.relationship;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsRetrievalStage;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;

import java.util.*;

import static pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator.anonymizeObjectIfNeeded;

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

        AccessControlEvaluator accessControlEvaluator
                = processorContext.getAccessControlEvaluator();

        CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse = new MultipleDataItemsRetrievalStage(
                accessControlEvaluator,
                processorContext.getInboundAccessControlSettings()
        ).retrieveData(request, dataSupplier);

        // return if downstream response is null or inbound access is not allowed or the response is null
        if (cursorPageableResponse == null) {
            // top-level links
            LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, null, null);
            // top-level meta
            Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, null);
            // compose doc
            return docSupplier.get(Collections.emptyList(), docLinks, docMeta);
        }

        //
        // Outbound Access Control checks + doc composing
        //
        Map<DATA_SOURCE_DTO, ResourceIdentifierObject> anonymizationResultMap = new LinkedHashMap<>();
        for (DATA_SOURCE_DTO dto: cursorPageableResponse.getItems()) {
            // id and type
            IdAndType idAndType = jsonApiMembersResolver.resolveResourceTypeAndId(dto);
            // resource identifier meta
            Object resourceIdentifierMeta = jsonApiMembersResolver.resolveResourceMeta(request, dto);
            // compose resource identifier
            ResourceIdentifierObject resourceIdentifierObject = new ResourceIdentifierObject(
                    idAndType.getId(),
                    idAndType.getType().getType(),
                    resourceIdentifierMeta
            );

            // anonymize
            AnonymizationResult<ResourceIdentifierObject> anonymizationResult = anonymizeObjectIfNeeded(
                    accessControlEvaluator,
                    resourceIdentifierObject,
                    resourceIdentifierObject,
                    Optional.ofNullable(processorContext.getOutboundAccessControlSettings())
                            .map(OutboundAccessControlForJsonApiResourceIdentifier::toOutboundRequirementsForCustomClass)
                            .orElse(null)
            );

            anonymizationResultMap.put(dto, anonymizationResult.targetObject());
        }

        List<DATA_SOURCE_DTO> nonAnonymizedDtos = anonymizationResultMap.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(Map.Entry::getKey)
                .toList();

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(
                request,
                nonAnonymizedDtos,
                cursorPageableResponse.getNextCursor()
        );
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, nonAnonymizedDtos);

        // compose data
        List<ResourceIdentifierObject> data = anonymizationResultMap.values().stream().filter(Objects::nonNull).toList();

        // compose response
        return docSupplier.get(data, docLinks, docMeta);
    }

    public ToManyRelationshipsDoc toToManyRelationshipsDoc() {
        return toToManyRelationshipsDoc(ToManyRelationshipsDoc::new);
    }

}
