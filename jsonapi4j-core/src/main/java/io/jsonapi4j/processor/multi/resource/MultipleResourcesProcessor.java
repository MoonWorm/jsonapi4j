package io.jsonapi4j.processor.multi.resource;

/**
 * Common JSON:API compatible utility processor that aimed to help to automate common use cases e.g.
 * Request -> Response pipelines.
 */
public class MultipleResourcesProcessor {

    public MultipleResourcesProcessor() {

    }

    /**
     * Initializes the processor and enables the first configuration stage - processor-specific configurations
     *
     * @param request object that encapsulates all the needed input data for execution
     */
    public <REQUEST> MultipleResourcesProcessorConfigurationStage<REQUEST> forRequest(REQUEST request) {
        return new MultipleResourcesProcessorConfigurationStage<>(request);
    }

    /**
     * Initializes the processor and enables the first configuration stage - processor-specific configurations
     * <p/>
     * Can be used if no request is required. Its developer responsibility to avoid NPEs and ensure that the request
     * reference for the next execution steps will be null.
     */
    public <REQUEST> MultipleResourcesProcessorConfigurationStage<REQUEST> noRequest() {
        return new MultipleResourcesProcessorConfigurationStage<>(null);
    }

}
