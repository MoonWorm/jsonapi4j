package io.jsonapi4j.config;

import io.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class CompoundDocsProperties {

    public static final int JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE = 2;
    public static final CompoundDocsResolverConfig.ErrorStrategy JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE = CompoundDocsResolverConfig.ErrorStrategy.IGNORE;

    private boolean enabled = false;
    private int maxHops = JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE;
    private CompoundDocsResolverConfig.ErrorStrategy errorStrategy = JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE;
    private Map<String, String> mapping = Collections.emptyMap();
}
