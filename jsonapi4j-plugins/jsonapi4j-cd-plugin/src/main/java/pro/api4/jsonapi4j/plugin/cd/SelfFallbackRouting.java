package pro.api4.jsonapi4j.plugin.cd;

import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.Transport;

import java.net.URI;
import java.util.Optional;

/**
 * Routing for the CD plugin: the configured {@link DomainSettingsResolver}, with every resource type it has no route for
 * served by this app itself.
 *
 * <p>Those types are fetched per their {@link Transport}: in-process by default - see {@link InProcessBatchFetcher} - or
 * over HTTP from {@code jsonapi4j.cd.mapping.default.url}, this app's own address, for an app whose servlet filters -
 * e.g. URL-based security rules - have to see include requests too. Fetching from this app itself stays within the same trust boundary, so those
 * fetches carry the client's credentials; a configured route carries them only when the configured resolver says so.
 */
public class SelfFallbackRouting implements DomainSettingsResolver {

    private final DomainSettingsResolver domainSettingsResolver;
    private final CompoundDocsProperties cdProperties;
    private final URI defaultMapping;

    /**
     * @param domainSettingsResolver the configured routing
     * @param cdProperties           supplies the batch size for same-app fetches, and {@code mapping.default.url}
     */
    public SelfFallbackRouting(DomainSettingsResolver domainSettingsResolver, CompoundDocsProperties cdProperties) {
        this.domainSettingsResolver = domainSettingsResolver;
        this.cdProperties = cdProperties;
        this.defaultMapping = cdProperties.defaultMapping().map(URI::create).orElse(null);
    }

    /**
     * A {@code null} from the configured resolver breaks its contract and is passed on as is, so
     * {@link DomainSettingsResolver#requireDomainSettings(String)} reports it rather than it being taken for "no route".
     *
     * @return the configured route when there is one, otherwise this app - in-process, or at
     * {@code jsonapi4j.cd.mapping.default.url} when set
     */
    @Override
    public Optional<DomainSettings> resolveDomainSettings(String resourceType) {
        Optional<DomainSettings> configured = domainSettingsResolver.resolveDomainSettings(resourceType);
        if (configured == null) {
            return null;
        }
        return configured.or(() -> Optional.of(selfDomainSettings(resourceType)));
    }

    private DomainSettings selfDomainSettings(String resourceType) {
        int maxBatchSize = cdProperties.maxBatchSize(resourceType);
        if (cdProperties.transportOf(resourceType) == Transport.IN_PROCESS) {
            return DomainSettings.inProcess(maxBatchSize);
        }
        if (defaultMapping == null) {
            throw new DomainResolutionException(String.format(
                    "Resource type '%s' is fetched over HTTP, but 'mapping.default.url' is not set", resourceType
            ));
        }
        return DomainSettings.overHttp(defaultMapping, maxBatchSize, true);
    }

}
