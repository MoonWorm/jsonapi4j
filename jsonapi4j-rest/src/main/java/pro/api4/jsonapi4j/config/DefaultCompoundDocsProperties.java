package pro.api4.jsonapi4j.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.compound.docs.ErrorStrategy;

import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultCompoundDocsProperties implements CompoundDocsProperties {

    private boolean enabled = Boolean.parseBoolean(JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE);
    private int maxHops = Integer.parseInt(JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE);
    private ErrorStrategy errorStrategy = ErrorStrategy.valueOf(JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE);
    private Map<String, String> mapping = JSONAPI4J_COMPOUND_DOCS_MAPPING_DEFAULT_VALUE;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public int maxHops() {
        return maxHops;
    }

    @Override
    public ErrorStrategy errorStrategy() {
        return errorStrategy;
    }

    @Override
    public Map<String, String> mapping() {
        return mapping;
    }

}
