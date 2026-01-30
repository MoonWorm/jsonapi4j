package pro.api4.jsonapi4j.processor.single.relationship;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext.RelationshipProcessorContextBuilder;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;

import java.util.List;

public class ToOneRelationshipProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private final RelationshipProcessorContextBuilder processorContextBuilder;

    ToOneRelationshipProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContextBuilder = RelationshipProcessorContext.builder();
    }

    public ToOneRelationshipProcessorConfigurationStage<REQUEST> plugins(List<PluginSettings> plugins) {
        this.processorContextBuilder.plugins(plugins);
        return this;
    }

    public <DATA_SOURCE_DTO> ToOneRelationshipJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> dataSupplier(
            SingleDataItemSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier
    ) {
        Validate.notNull(dataSupplier);
        return new ToOneRelationshipJsonApiConfigurationStage<>(
                request,
                dataSupplier,
                processorContextBuilder.build()
        );
    }


}
