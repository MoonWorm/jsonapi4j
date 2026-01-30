package pro.api4.jsonapi4j.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ResourceProcessorContext {

    public static final Executor DEFAULT_EXECUTOR = Runnable::run; // no parallelization

    @Builder.Default
    private Executor executor = DEFAULT_EXECUTOR;
    @Builder.Default
    private List<PluginSettings> plugins = Collections.emptyList();

    // TODO: remove
    private AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;
    private OutboundAccessControlForJsonApiResource outboundAccessControlSettings;

}
