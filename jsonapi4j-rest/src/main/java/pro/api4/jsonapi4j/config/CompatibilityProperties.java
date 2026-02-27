package pro.api4.jsonapi4j.config;

import lombok.Data;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class CompatibilityProperties {

    private boolean legacyMode = false;
    private Set<String> supportedExtensions = new LinkedHashSet<>();
    private Set<String> supportedProfiles = new LinkedHashSet<>();

    public JsonApi4jCompatibilityMode resolveMode() {
        return JsonApi4jCompatibilityMode.fromLegacyMode(legacyMode);
    }

    public Set<String> resolveSupportedExtensions() {
        return normalizeUris(supportedExtensions);
    }

    public Set<String> resolveSupportedProfiles() {
        return normalizeUris(supportedProfiles);
    }

    private Set<String> normalizeUris(Set<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return Collections.emptySet();
        }
        return uris.stream()
                .filter(uri -> uri != null && !uri.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
