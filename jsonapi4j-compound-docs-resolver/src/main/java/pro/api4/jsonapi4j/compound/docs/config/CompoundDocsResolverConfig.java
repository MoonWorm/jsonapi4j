package pro.api4.jsonapi4j.compound.docs.config;

public class CompoundDocsResolverConfig {

    private final boolean enabled;
    private final int maxHops;
    private final ErrorStrategy errorStrategy;

    public CompoundDocsResolverConfig(boolean enabled,
                                      int maxHops,
                                      ErrorStrategy errorStrategy) {
        this.enabled = enabled;
        this.maxHops = maxHops;
        this.errorStrategy = errorStrategy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxHops() {
        return maxHops;
    }

    public ErrorStrategy getErrorStrategy() {
        return errorStrategy;
    }

    @Override
    public String toString() {
        return "CompoundDocsResolverConfig{" +
                "enabled=" + enabled +
                ", maxHops=" + maxHops +
                ", errorStrategy=" + errorStrategy +
                '}';
    }
}
