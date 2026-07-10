package pro.api4.jsonapi4j.plugin.cd.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.config.RawConfigAccessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultCompoundDocsProperties implements CompoundDocsProperties {

    private boolean enabled = Boolean.parseBoolean(DEFAULT_ENABLED);
    private int maxHops = Integer.parseInt(DEFAULT_MAX_HOPS);
    private int maxIncludedResources = Integer.parseInt(DEFAULT_MAX_INCLUDED_RESOURCES);
    private ErrorStrategy errorStrategy = ErrorStrategy.valueOf(DEFAULT_ERROR_STRATEGY);
    private Map<String, String> mapping = Collections.emptyMap();
    private Map<String, Integer> batchSizeMapping = Collections.emptyMap();
    private int defaultMaxBatchSize = Integer.parseInt(DEFAULT_MAX_BATCH_SIZE);
    private List<Propagation> propagation = parsePropagationString(DEFAULT_PROPAGATION);
    private boolean deduplicateResources = Boolean.parseBoolean(DEFAULT_DEDUPLICATE_RESOURCES);
    private long httpConnectTimeoutMs = Long.parseLong(DEFAULT_HTTP_CONNECT_TIMEOUT_MS);
    private long httpTotalTimeoutMs = Long.parseLong(DEFAULT_HTTP_TOTAL_TIMEOUT_MS);
    private DefaultCache cache;

    @Getter
    @Setter
    public static class DefaultCache implements Cache {

        private boolean enabled = Boolean.parseBoolean(DEFAULT_CACHE_ENABLED);
        private int maxSize = Integer.parseInt(DEFAULT_CACHE_MAX_SIZE);

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public int maxSize() {
            return maxSize;
        }
    }

    public static CompoundDocsProperties toCdProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        return new RawConfigAccessor(jsonApi4jPropertiesRaw)
                .section(CompoundDocsProperties.CD_PROPERTY)
                .map(RawConfigAccessor::getProperties)
                .filter(cd -> !cd.isEmpty())
                .map(cd -> JsonApi4jConfigReader.convertToConfig(cd, DefaultCompoundDocsProperties.class))
                .orElseGet(DefaultCompoundDocsProperties::new);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public int maxHops() {
        return maxHops;
    }

    @Override
    public int maxIncludedResources() {
        return maxIncludedResources;
    }

    @Override
    public ErrorStrategy errorStrategy() {
        return errorStrategy;
    }

    @Override
    public Map<String, String> mapping() {
        return mapping;
    }

    @Override
    public Map<String, Integer> batchSizeMapping() {
        return batchSizeMapping;
    }

    @Override
    public int defaultMaxBatchSize() {
        return defaultMaxBatchSize;
    }

    @Override
    public List<Propagation> propagation() {
        return propagation;
    }

    @Override
    public boolean deduplicateResources() {
        return deduplicateResources;
    }

    @Override
    public long httpConnectTimeoutMs() {
        return httpConnectTimeoutMs;
    }

    @Override
    public long httpTotalTimeoutMs() {
        return httpTotalTimeoutMs;
    }

    @Override
    public Cache cache() {
        return cache;
    }
}
