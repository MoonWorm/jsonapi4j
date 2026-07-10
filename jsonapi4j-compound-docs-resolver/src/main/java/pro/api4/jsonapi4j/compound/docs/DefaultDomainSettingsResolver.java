package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Default {@link DomainSettingsResolver} backed by per-resource-type maps for base URLs and batch sizes.
 *
 * <p>An explicit {@code jsonapi4j.cd.mapping.<type>} entry always wins and is only needed for resource types served by
 * a <em>different</em> service. Any unmapped type is treated as same-app and resolved against {@code selfBaseUrl} — the
 * app's own JSON:API root derived from the incoming request — so same-app types (including the built-in meta types)
 * resolve with no configuration. Resource types without an explicit batch size override fall back to
 * {@code defaultMaxBatchSize}.
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

    /**
     * An explicit mapping always wins (federated/remote types). An unmapped type resolves against {@code selfBaseUrl} —
     * the app's own JSON:API root derived from the incoming request — so same-app types (including the meta types)
     * resolve against the exact endpoint the request arrived on. {@code selfBaseUrl} is required for unmapped types (it
     * is guaranteed non-null by {@link CompoundDocsRequest}).
     */
    @Override
    public DomainSettings resolveDomainSettings(String resourceType, String selfBaseUrl) {
        URI url = mappings.get(resourceType);
        if (url == null) {
            url = URI.create(selfBaseUrl);
        }
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
