package pro.api4.jsonapi4j.compound.docs.config;

import lombok.Data;

import java.util.List;

@Data
public class CompoundDocsResolverConfig {

    private final boolean enabled;
    private final int maxHops;
    private final int maxIncludedResources;
    private final ErrorStrategy errorStrategy;
    private final List<Propagation> propagation;
    private final boolean deduplicateResources;
    private final long httpConnectTimeoutMs;
    private final long httpTotalTimeoutMs;
    private final boolean cacheEnabled;
    private final int cacheMaxSize;

}
