package pro.api4.jsonapi4j.compound.docs.config;

import java.util.List;

public class CompoundDocsResolverConfig {

    private final boolean enabled;
    private final int maxHops;
    private final int maxIncludedResources;
    private final ErrorStrategy errorStrategy;
    private final List<Propagation> propagation;
    private final long httpConnectTimeoutMs;
    private final long httpTotalTimeoutMs;

    public CompoundDocsResolverConfig(boolean enabled,
                                      int maxHops,
                                      int maxIncludedResources,
                                      ErrorStrategy errorStrategy,
                                      List<Propagation> propagation,
                                      long httpConnectTimeoutMs,
                                      long httpTotalTimeoutMs) {
        this.enabled = enabled;
        this.maxHops = maxHops;
        this.maxIncludedResources = maxIncludedResources;
        this.errorStrategy = errorStrategy;
        this.propagation = propagation;
        this.httpConnectTimeoutMs = httpConnectTimeoutMs;
        this.httpTotalTimeoutMs = httpTotalTimeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxHops() {
        return maxHops;
    }

    public int getMaxIncludedResources() {
        return maxIncludedResources;
    }

    public ErrorStrategy getErrorStrategy() {
        return errorStrategy;
    }

    public List<Propagation> getPropagation() {
        return propagation;
    }

    public long getHttpConnectTimeoutMs() {
        return httpConnectTimeoutMs;
    }

    public long getHttpTotalTimeoutMs() {
        return httpTotalTimeoutMs;
    }

    @Override
    public String toString() {
        return "CompoundDocsResolverConfig{" +
                "enabled=" + enabled +
                ", maxHops=" + maxHops +
                ", maxIncludedResources=" + maxIncludedResources +
                ", errorStrategy=" + errorStrategy +
                ", propagation=" + propagation +
                ", httpConnectTimeoutMs=" + httpConnectTimeoutMs +
                ", httpTotalTimeoutMs=" + httpTotalTimeoutMs +
                '}';
    }

}
