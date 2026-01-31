package pro.api4.jsonapi4j.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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

}
