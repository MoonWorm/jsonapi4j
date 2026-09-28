package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Default {@link DomainSettingsResolver} backed by per-resource-type maps for base URLs and batch sizes.
 *
 * <p>Only mapped resource types have a route; any other type resolves to empty. Resource types without an explicit batch
 * size override fall back to {@code defaultMaxBatchSize}.
 */
public class DefaultDomainSettingsResolver implements DomainSettingsResolver {

    private final Map<String, URI> mappings;
    private final Map<String, Integer> batchSizeMappings;
    private final int defaultMaxBatchSize;

    public DefaultDomainSettingsResolver(Map<String, URI> mappings,
                                         Map<String, Integer> batchSizeMappings,
                                         int defaultMaxBatchSize) {
        this.mappings = mappings;
        this.batchSizeMappings = batchSizeMappings;
        this.defaultMaxBatchSize = defaultMaxBatchSize;
    }

    public static DomainSettingsResolver from(Map<String, String> mappings,
                                              Map<String, Integer> batchSizeMappings,
                                              int defaultMaxBatchSize) {
        return new DefaultDomainSettingsResolver(
                toUriMap(mappings),
                batchSizeMappings,
                defaultMaxBatchSize
        );
    }

    @Override
    public Optional<DomainSettings> resolveDomainSettings(String resourceType) {
        return Optional.ofNullable(mappings.get(resourceType))
                .map(url -> new DomainSettings(url, maxBatchSize(resourceType)));
    }

    private int maxBatchSize(String resourceType) {
        return batchSizeMappings.getOrDefault(resourceType, defaultMaxBatchSize);
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
