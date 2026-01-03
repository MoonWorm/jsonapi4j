package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.plugin.ac.impl.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.impl.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator;
import org.apache.commons.lang3.Validate;

public class ToManyRelationshipsProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private RelationshipProcessorContext processorContext;

    ToManyRelationshipsProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContext = new RelationshipProcessorContext();
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.processorContext = this.processorContext.withAccessControlEvaluator(accessControlEvaluator);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControl
    ) {
        this.processorContext = this.processorContext.withOutboundAccessControlSettings(outboundAccessControl);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> inboundAccessControlSettings(
            AccessControlModel inboundAccessControlSettings
    ) {
        this.processorContext = this.processorContext.withInboundAccessControlSettings(inboundAccessControlSettings);
        return this;
    }

    public <DATA_SOURCE_DTO> ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> dataSupplier(
            MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier
    ) {
        Validate.notNull(dataSupplier);
        return new ToManyRelationshipsJsonApiConfigurationStage<>(
                request,
                dataSupplier,
                processorContext
        );
    }


}
