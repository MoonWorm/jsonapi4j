package pro.api4.jsonapi4j.compound.docs;

import java.util.Map;
import java.util.Optional;

/**
 * Default {@link DomainSettingsResolver} backed by the {@link DomainSettings} of each mapped resource type - its base
 * URL, batch size and whether it gets the client's credentials. Any other type resolves to empty, i.e. has no route.
 */
public class DefaultDomainSettingsResolver implements DomainSettingsResolver {

    private final Map<String, DomainSettings> domainSettings;

    /**
     * @param domainSettings the settings of each mapped resource type, by resource type
     */
    public DefaultDomainSettingsResolver(Map<String, DomainSettings> domainSettings) {
        this.domainSettings = Map.copyOf(domainSettings);
    }

    @Override
    public Optional<DomainSettings> resolveDomainSettings(String resourceType) {
        return Optional.ofNullable(domainSettings.get(resourceType));
    }

}
