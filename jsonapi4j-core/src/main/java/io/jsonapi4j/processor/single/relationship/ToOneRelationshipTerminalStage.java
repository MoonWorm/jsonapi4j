package io.jsonapi4j.processor.single.relationship;

import io.jsonapi4j.processor.IdAndType;
import io.jsonapi4j.processor.RelationshipProcessorContext;
import io.jsonapi4j.processor.ac.InboundAccessControlRequerementsEvaluator;
import io.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForRelationship;
import io.jsonapi4j.processor.single.SingleDataItemSupplier;
import io.jsonapi4j.processor.util.DataRetrievalUtil;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ResourceIdentifierObject;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

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
        IdAndType idAndType = jsonApiMembersResolver.resolveResourceTypeAndId(dataSourceDto);
        // resource meta
        Object resourceMeta = jsonApiMembersResolver.resolveResourceMeta(request, dataSourceDto);
        // compose data
        ResourceIdentifierObject data = new ResourceIdentifierObject(
                idAndType.getId(),
                idAndType.getType().getType(),
                resourceMeta
        );

        //
        // Outbound Access Control checks
        //
        OutboundAccessControlRequirementsEvaluatorForRelationship outboundAcEvaluator = new OutboundAccessControlRequirementsEvaluatorForRelationship(
                processorContext.getAccessControlEvaluator(),
                processorContext.getOutboundAccessControlSettings()
        );
        // anonymize resource identifier if needed
        data = outboundAcEvaluator.anonymizeResourceIdentifierIfNeeded(data);

        // compose response
        return docSupplier.get(data, docLinks, docMeta);
    }

    public ToOneRelationshipDoc toToOneRelationshipDoc() {
        return toToOneRelationshipDoc(ToOneRelationshipDoc::new);
    }

}
