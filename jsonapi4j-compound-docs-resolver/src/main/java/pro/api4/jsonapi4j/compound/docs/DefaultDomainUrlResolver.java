package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class DefaultDomainUrlResolver implements DomainUrlResolver {

    private final Map<String, URI> mappings;
    private URI defaultDomainUrl = URI.create("http://localhost:8080");

    public DefaultDomainUrlResolver(Map<String, URI> mappings) {
        this.mappings = mappings;
    }

    public DefaultDomainUrlResolver(Map<String, URI> mappings,
                                    URI defaultDomainUrl) {
        this.mappings = mappings;
        this.defaultDomainUrl = defaultDomainUrl;
    }

    public static DomainUrlResolver from(Map<String, String> mappings) {
        return new DefaultDomainUrlResolver(
                mappings.entrySet()
                        .stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                e -> URI.create(e.getValue())
                        ))
        );
    }

    @Override
    public URI getDomainUrl(String resourceType) {
        URI result = mappings.get(resourceType);
        if (result != null) {
            return result;
        }
        return defaultDomainUrl;
    }
}
