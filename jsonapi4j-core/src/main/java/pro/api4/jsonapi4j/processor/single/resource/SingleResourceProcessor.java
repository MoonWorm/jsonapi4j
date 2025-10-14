package pro.api4.jsonapi4j.processor.single.resource;

/**
 * JSON:API compatible utility processor that must be used for scenarios that expect a single-primary-resource JSON:API
 * document as a response.
 * <p/>
 * Single-resource JSON:API document is the document that has a single resource inside 'data' property and must be
 * wrapped with the curve brackets on the JSON level:
 * <p/>
 * <code>{"data" : {...}}</code>
 * <p/>
 * This processor is compatible with the Access Control plugin that allows to filter out objects/field on a different
 * JSON:API document levels (e.g. resource, attributes, relationships).
 */
public class SingleResourceProcessor {

    public SingleResourceProcessor() {

    }

    /**
     * Initializes the processor and enables the first configuration stage - processor-specific configurations
     *
     * @param request object that encapsulates all the needed input data for execution
     */
    public <REQUEST> SingleResourceProcessorConfigurationStage<REQUEST> forRequest(REQUEST request) {
        return new SingleResourceProcessorConfigurationStage<>(request);
    }

    /**
     * Initializes the processor and enables the first configuration stage - processor-specific configurations
     * <p/>
     * Can be used if no request is required. Its developer responsibility to avoid NPEs and ensure that the request
     * reference for the next execution steps will be null.
     */
    public <REQUEST> SingleResourceProcessorConfigurationStage<REQUEST> noRequest() {
        return new SingleResourceProcessorConfigurationStage<>(null);
    }

}
