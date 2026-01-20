package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;

import java.util.concurrent.Executor;

@Getter
@With
public class ResourceProcessorContext {

    public static final Executor DEFAULT_EXECUTOR = Runnable::run; // no parallelization

    private Executor executor;
    private AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;
    private OutboundAccessControlForJsonApiResource outboundAccessControlSettings;

    public ResourceProcessorContext(Executor executor,
                                    AccessControlEvaluator accessControlEvaluator,
                                    AccessControlModel inboundAccessControlSettings,
                                    OutboundAccessControlForJsonApiResource outboundAccessControlSettings) {

        this(executor);
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        this.outboundAccessControlSettings = outboundAccessControlSettings;
        this.accessControlEvaluator = accessControlEvaluator;
    }

    public ResourceProcessorContext(AccessControlEvaluator accessControlEvaluator,
                                    AccessControlModel inboundAccessControlSettings,
                                    OutboundAccessControlForJsonApiResource outboundAccessControlSettings) {

        this(DEFAULT_EXECUTOR);
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        this.outboundAccessControlSettings = outboundAccessControlSettings;
        this.accessControlEvaluator = accessControlEvaluator;
    }

    public ResourceProcessorContext(Executor executor) {
        Validate.notNull(executor);
        this.executor = executor;
    }

    public ResourceProcessorContext() {
        this.executor = DEFAULT_EXECUTOR;
    }

}
