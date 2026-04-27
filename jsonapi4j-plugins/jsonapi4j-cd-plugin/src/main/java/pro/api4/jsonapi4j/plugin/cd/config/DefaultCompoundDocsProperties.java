package pro.api4.jsonapi4j.plugin.cd.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;

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
        Object cdPropertiesObject = jsonApi4jPropertiesRaw.get(CompoundDocsProperties.CD_PROPERTY_NAME);
        Map<String, Object> cdPropertiesRaw = Collections.emptyMap();
        if (cdPropertiesObject instanceof Map cdPropertiesMap) {
            //noinspection unchecked
            cdPropertiesRaw = cdPropertiesMap;
        }
        CompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
        if (!cdPropertiesRaw.isEmpty()) {
            cdProperties = JsonApi4jConfigReader.convertToConfig(
                    cdPropertiesRaw,
                    DefaultCompoundDocsProperties.class
            );
        }
        return cdProperties;
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
