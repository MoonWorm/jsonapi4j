package pro.api4.jsonapi4j.processor.multi.relationship;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext;
import pro.api4.jsonapi4j.processor.RelationshipProcessorContext.RelationshipProcessorContextBuilder;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;

import java.util.List;

public class ToManyRelationshipsProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;

    private final RelationshipProcessorContextBuilder processorContextBuilder;

    ToManyRelationshipsProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContextBuilder = RelationshipProcessorContext.builder();
    }

    public ToManyRelationshipsProcessorConfigurationStage<REQUEST> plugins(List<PluginSettings> plugins) {
        this.processorContextBuilder.plugins(plugins);
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
