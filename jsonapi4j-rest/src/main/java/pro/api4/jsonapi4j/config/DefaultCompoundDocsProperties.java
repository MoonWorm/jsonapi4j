package pro.api4.jsonapi4j.config;

import lombok.Setter;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;

import java.util.Map;

@Setter
public class DefaultCompoundDocsProperties implements CompoundDocsProperties {

    private boolean enabled;
    private int maxHops;
    private CompoundDocsResolverConfig.ErrorStrategy errorStrategy;
    private Map<String, String> mapping;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public int maxHops() {
        return maxHops;
    }

    @Override
    public CompoundDocsResolverConfig.ErrorStrategy errorStrategy() {
        return errorStrategy;
    }

    @Override
    public Map<String, String> mapping() {
        return mapping;
    }

}
