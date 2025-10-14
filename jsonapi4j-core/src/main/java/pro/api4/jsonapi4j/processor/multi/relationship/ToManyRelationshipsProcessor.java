package pro.api4.jsonapi4j.processor.multi.relationship;

public class ToManyRelationshipsProcessor {

    public ToManyRelationshipsProcessor() {

    }

    public <REQUEST> ToManyRelationshipsProcessorConfigurationStage<REQUEST> forRequest(REQUEST request) {
        return new ToManyRelationshipsProcessorConfigurationStage<>(request);
    }

    public <REQUEST> ToManyRelationshipsProcessorConfigurationStage<REQUEST> noRequest() {
        return new ToManyRelationshipsProcessorConfigurationStage<>(null);
    }

}
