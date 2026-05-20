package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Default {@link DomainSettingsResolver} backed by per-resource-type maps for base URLs and batch
 * sizes. Unmapped resource types fall back to {@code defaultDomainUrl}; resource types without an
 * explicit batch size override fall back to {@code defaultMaxBatchSize}.
 */
public class DefaultDomainSettingsResolver implements DomainSettingsResolver {

    private final Map<String, URI> mappings;
    private final Map<String, Integer> batchSizeMappings;
    private final URI defaultDomainUrl;
    private final int defaultMaxBatchSize;

    public DefaultDomainSettingsResolver(Map<String, URI> mappings,
                                         Map<String, Integer> batchSizeMappings,
                                         URI defaultDomainUrl,
                                         int defaultMaxBatchSize) {
        this.mappings = mappings;
        this.batchSizeMappings = batchSizeMappings;
        this.defaultDomainUrl = defaultDomainUrl;
        this.defaultMaxBatchSize = defaultMaxBatchSize;
    }

    public static DomainSettingsResolver from(Map<String, String> mappings,
                                              Map<String, Integer> batchSizeMappings,
                                              int defaultMaxBatchSize) {
        return new DefaultDomainSettingsResolver(
                toUriMap(mappings),
                batchSizeMappings,
                URI.create("http://localhost:8080"),
                defaultMaxBatchSize
        );
    }

    @Override
    public DomainSettings resolveDomainSettings(String resourceType) {
        URI url = mappings.getOrDefault(resourceType, defaultDomainUrl);
        int batchSize = batchSizeMappings.getOrDefault(resourceType, defaultMaxBatchSize);
        return new DomainSettings(url, batchSize);
    }

    private static Map<String, URI> toUriMap(Map<String, String> mappings) {
        return mappings.entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> URI.create(e.getValue())
                ));
    }
}
