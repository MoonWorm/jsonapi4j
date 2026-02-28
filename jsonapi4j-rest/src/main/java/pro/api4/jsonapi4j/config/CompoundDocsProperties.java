package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class CompoundDocsProperties {

    public static final boolean JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE = false;
    public static final int JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE = 2;
    public static final CompoundDocsResolverConfig.ErrorStrategy JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE = CompoundDocsResolverConfig.ErrorStrategy.IGNORE;
    public static final Map<String, String> JSONAPI4J_COMPOUND_DOCS_MAPPING_DEFAULT_VALUE = Collections.emptyMap();

    private boolean enabled = JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE;
    private int maxHops = JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE;
    private CompoundDocsResolverConfig.ErrorStrategy errorStrategy = JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE;
    private Map<String, String> mapping = JSONAPI4J_COMPOUND_DOCS_MAPPING_DEFAULT_VALUE;
}
