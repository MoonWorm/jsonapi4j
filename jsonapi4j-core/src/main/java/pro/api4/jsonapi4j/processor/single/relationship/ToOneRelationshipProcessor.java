package pro.api4.jsonapi4j.processor.single.relationship;

public class ToOneRelationshipProcessor {

    public ToOneRelationshipProcessor() {

    }

    public <REQUEST> ToOneRelationshipProcessorConfigurationStage<REQUEST> forRequest(REQUEST request) {
        return new ToOneRelationshipProcessorConfigurationStage<>(request);
    }

    public <REQUEST> ToOneRelationshipProcessorConfigurationStage<REQUEST> noRequest() {
        return new ToOneRelationshipProcessorConfigurationStage<>(null);
    }

}
