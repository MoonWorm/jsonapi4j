package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import org.apache.commons.lang3.Validate;

public class  ToOneRelationshipProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private RelationshipProcessorContext processorContext;

    ToOneRelationshipProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContext = new RelationshipProcessorContext();
    }

    public ToOneRelationshipProcessorConfigurationStage<REQUEST> accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.processorContext = this.processorContext.withAccessControlEvaluator(accessControlEvaluator);
        return this;
    }

    public ToOneRelationshipProcessorConfigurationStage<REQUEST> outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
    ) {
        this.processorContext = this.processorContext.withOutboundAccessControlSettings(outboundAccessControlSettings);
        return this;
    }

    public ToOneRelationshipProcessorConfigurationStage<REQUEST> inboundAccessControlSettings(
            AccessControlModel inboundAccessControlSettings
    ) {
        this.processorContext = this.processorContext.withInboundAccessControlSettings(inboundAccessControlSettings);
        return this;
    }

    public <DATA_SOURCE_DTO> ToOneRelationshipJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> dataSupplier(
            SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier
    ) {
        Validate.notNull(dataSupplier);
        return new ToOneRelationshipJsonApiConfigurationStage<>(
                request,
                dataSupplier,
                processorContext
        );
    }


}
