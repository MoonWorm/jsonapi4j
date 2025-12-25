package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.impl.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.impl.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.single.SingleDataItemsRetrievalStage;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

import static pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator.anonymizeObjectIfNeeded;

@Slf4j
public class ToOneRelationshipTerminalStage<REQUEST, DATA_SOURCE_DTO> {

    private final REQUEST request;
    private final SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier;
    private final RelationshipProcessorContext processorContext;
    private final ToOneRelationshipJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> jsonApiMembersResolver;

    ToOneRelationshipTerminalStage(REQUEST request,
                                   SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier,
                                   RelationshipProcessorContext processorContext,
                                   ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext) {
        this.request = request;
        this.dataSupplier = dataSupplier;
        this.processorContext = processorContext;
        this.jsonApiMembersResolver = new ToOneRelationshipJsonApiMembersResolver<>(
                jsonApiContext
        );
    }

    public <DOC extends ToOneRelationshipDoc> DOC toToOneRelationshipDoc(
            ToOneRelationshipDocSupplier<DOC> docSupplier
    ) {
        // validation
        Validate.notNull(docSupplier);

        AccessControlEvaluator accessControlEvaluator
                = processorContext.getAccessControlEvaluator();

        DATA_SOURCE_DTO dataSourceDto = new SingleDataItemsRetrievalStage(
                accessControlEvaluator,
                processorContext.getInboundAccessControlSettings()
        ).retrieveData(request, dataSupplier);

        // top-level links
        LinksObject docLinks = jsonApiMembersResolver.resolveDocLinks(request, dataSourceDto);
        // top-level meta
        Object docMeta = jsonApiMembersResolver.resolveDocMeta(request, dataSourceDto);

        // return if downstream response is null or inbound access is not allowed
        if (dataSourceDto == null) {
            return docSupplier.get(null, docLinks, docMeta);
        }

        // resource id and type
        IdAndType idAndType = jsonApiMembersResolver.resolveResourceTypeAndId(dataSourceDto);
        // resource meta
        Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(request, dataSourceDto);
        // compose data
        ResourceIdentifierObject resourceIdentifierObject = new ResourceIdentifierObject(
                idAndType.getId(),
                idAndType.getType().getType(),
                resourceMeta
        );

        AnonymizationResult<ResourceIdentifierObject> anonymizationResult = anonymizeObjectIfNeeded(
                accessControlEvaluator,
                resourceIdentifierObject,
                resourceIdentifierObject,
                Optional.ofNullable(processorContext.getOutboundAccessControlSettings())
                        .map(OutboundAccessControlForJsonApiResourceIdentifier::toOutboundRequirementsForCustomClass)
                        .orElse(null)
        );

        // anonymize resource identifier if needed
        ResourceIdentifierObject data = anonymizationResult.targetObject();

        // compose response
        return docSupplier.get(data, docLinks, docMeta);
    }

    public ToOneRelationshipDoc toToOneRelationshipDoc() {
        return toToOneRelationshipDoc(ToOneRelationshipDoc::new);
    }

}
