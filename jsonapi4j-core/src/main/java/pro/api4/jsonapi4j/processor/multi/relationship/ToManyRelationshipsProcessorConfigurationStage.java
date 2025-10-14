package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlSettings;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlSettingsForRelationship;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import org.apache.commons.lang3.Validate;

public class ToManyRelationshipsProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private RelationshipProcessorContext processorContext;

    ToManyRelationshipsProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContext = RelationshipProcessorContext.DEFAULT;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.processorContext = this.processorContext.withAccessControlEvaluator(accessControlEvaluator);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> outboundAccessControlSettings(
            OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
    ) {
        this.processorContext = this.processorContext.withOutboundAccessControlSettings(outboundAccessControlSettings);
        return this;
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> inboundAccessControlSettings(
            InboundAccessControlSettings inboundAccessControlSettings
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
