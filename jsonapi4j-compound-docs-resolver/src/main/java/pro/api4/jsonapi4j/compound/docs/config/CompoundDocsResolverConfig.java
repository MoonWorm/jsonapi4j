package pro.api4.jsonapi4j.compound.docs.config;

import java.util.List;

public class CompoundDocsResolverConfig {

    private final boolean enabled;
    private final int maxHops;
    private final int maxIncludedResources;
    private final ErrorStrategy errorStrategy;
    private final List<Propagation> propagation;

    public CompoundDocsResolverConfig(boolean enabled,
                                      int maxHops,
                                      int maxIncludedResources,
                                      ErrorStrategy errorStrategy,
                                      List<Propagation> propagation) {
        this.enabled = enabled;
        this.maxHops = maxHops;
        this.maxIncludedResources = maxIncludedResources;
        this.errorStrategy = errorStrategy;
        this.propagation = propagation;
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

    @Override
    public String toString() {
        return "CompoundDocsResolverConfig{" +
                "enabled=" + enabled +
                ", maxHops=" + maxHops +
                ", maxIncludedResources=" + maxIncludedResources +
                ", errorStrategy=" + errorStrategy +
                ", propagation=" + propagation +
                '}';
    }
}
