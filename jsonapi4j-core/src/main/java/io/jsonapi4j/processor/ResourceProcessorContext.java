package io.jsonapi4j.processor;

import io.jsonapi4j.processor.ac.InboundAccessControlSettings;
import io.jsonapi4j.processor.ac.OutboundAccessControlSettingsForResource;
import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import io.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import io.jsonapi4j.plugin.ac.tier.DefaultAccessTierRegistry;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.Executor;

@Getter
@With
public class ResourceProcessorContext {

    public static final Executor DEFAULT_EXECUTOR = Runnable::run;
    public static final AccessControlEvaluator DEFAULT_ACCESS_CONTROL_EVALUATOR
            = new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry());
    public static final ResourceProcessorContext DEFAULT = new ResourceProcessorContext(
            DEFAULT_EXECUTOR,
            DEFAULT_ACCESS_CONTROL_EVALUATOR,
            InboundAccessControlSettings.DEFAULT,
            OutboundAccessControlSettingsForResource.DEFAULT
    );

    private final Executor executor;
    private final AccessControlEvaluator accessControlEvaluator;
    private final InboundAccessControlSettings inboundAccessControlSettings;
    private final OutboundAccessControlSettingsForResource outboundAccessControlSettings;

    public ResourceProcessorContext(Executor executor,
                                    AccessControlEvaluator accessControlEvaluator,
                                    InboundAccessControlSettings inboundAccessControlSettings,
                                    OutboundAccessControlSettingsForResource outboundAccessControlSettings) {
        Validate.notNull(executor);
        Validate.notNull(accessControlEvaluator);
        Validate.notNull(inboundAccessControlSettings);
        Validate.notNull(outboundAccessControlSettings);
        this.executor = executor;
        this.accessControlEvaluator = accessControlEvaluator;
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        this.outboundAccessControlSettings = outboundAccessControlSettings;
    }

}
