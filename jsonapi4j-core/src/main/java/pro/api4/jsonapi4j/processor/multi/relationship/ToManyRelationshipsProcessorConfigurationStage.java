package pro.api4.jsonapi4j.processor.multi.relationship;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext.RelationshipProcessorContextBuilder;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;

public class ToManyRelationshipsProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private final RelationshipProcessorContextBuilder processorContextBuilder;

    ToManyRelationshipsProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContextBuilder = RelationshipProcessorContext.builder();
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.processorContextBuilder.accessControlEvaluator(accessControlEvaluator);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControl
    ) {
        this.processorContextBuilder.outboundAccessControlSettings(outboundAccessControl);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> inboundAccessControlSettings(
            AccessControlModel inboundAccessControlSettings
    ) {
        this.processorContextBuilder.inboundAccessControlSettings(inboundAccessControlSettings);
        return this;
    }

    public <DATA_SOURCE_DTO> ToManyRelationshipsJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> dataSupplier(
            MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier
    ) {
        Validate.notNull(dataSupplier);
        return new ToManyRelationshipsJsonApiConfigurationStage<>(
                request,
                dataSupplier,
                processorContextBuilder.build()
        );
    }


}
