package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext.ResourceProcessorContextBuilder;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiConfigurationStage;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.Executor;

/**
 * @param <REQUEST> initial request type that must consists all the info that is needed for execution
 */
public class MultipleResourcesProcessorConfigurationStage<REQUEST> {

    private final REQUEST request;
    private ResourceProcessorContextBuilder processorContextBuilder;

    MultipleResourcesProcessorConfigurationStage(REQUEST request) {
        this.request = request;
        this.processorContextBuilder =  ResourceProcessorContext.builder();
    }

    /**
     * Optional. Enables concurrent processing of relationship data providers. Brings significant performance
     * optimization only if the corresponding JSON:API Resource has more than 1 relationship.
     * <p/>
     * No parallelization by default.
     *
     * @param executor any implementation of {@link Executor}, for example <code>Executors.newCachedThreadPool()</code>
     * @return self link, {@link #dataSupplier(MultipleDataItemsSupplier)} can be used afterward
     */
    public MultipleResourcesProcessorConfigurationStage<REQUEST> concurrentRelationshipResolution(
            Executor executor
    ) {
        this.processorContextBuilder = this.processorContextBuilder.executor(executor);
        return this;
    }

    public MultipleResourcesProcessorConfigurationStage<REQUEST> accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.processorContextBuilder = this.processorContextBuilder.accessControlEvaluator(accessControlEvaluator);
        return this;
    }

    public MultipleResourcesProcessorConfigurationStage<REQUEST> outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResource outboundAccessControlSettings
    ) {
        this.processorContextBuilder = this.processorContextBuilder.outboundAccessControlSettings(outboundAccessControlSettings);
        return this;
    }

    public MultipleResourcesProcessorConfigurationStage<REQUEST> inboundAccessControlSettings(
            AccessControlModel inboundAccessControlSettings
    ) {
        this.processorContextBuilder = this.processorContextBuilder.inboundAccessControlSettings(inboundAccessControlSettings);
        return this;
    }

    /**
     * Specifies related to primary resource data provider. Moves processor to the second configuration stage -
     * JSON:API spec configuration settings.
     *
     * @param dataSupplier      data supplier function, returns {@link CursorPageableResponse}, accepts parameter of type REQUEST
     * @param <DATA_SOURCE_DTO> data source dto type, usually represents intermediate dto type of data source service
     * @return the second configuration state - {@link SingleResourceJsonApiConfigurationStage}
     */
    public <DATA_SOURCE_DTO> MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> dataSupplier(
            MultipleDataItemsSupplier<REQUEST, DATA_SOURCE_DTO> dataSupplier
    ) {
        Validate.notNull(dataSupplier);
        return new MultipleResourcesJsonApiConfigurationStage<>(
                request,
                dataSupplier,
                processorContextBuilder.build()
        );
    }


}
