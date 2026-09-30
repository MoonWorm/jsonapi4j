package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.util.BaseUrls;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Routing for the CD plugin: the configured {@link DomainSettingsResolver}, with every resource type it has no route for
 * served by this app itself.
 *
 * <p>Those types are fetched from {@code jsonapi4j.cd.mapping.default} when it is set - e.g. through an environment
 * variable in a deployment where the app is not reachable on loopback as-is. Otherwise the app's own JSON:API root is
 * built per request - loopback, at the local port the request arrived on, plus the
 * servlet context path and the configured JSON:API root path. The loopback address matches the family the request
 * arrived over, since that proves the server listens on it: {@code [::1]} for IPv6, {@code 127.0.0.1} otherwise. It is
 * deliberately never taken from the {@code Host} or {@code X-Forwarded-*} headers: those are client-controlled, so
 * trusting them would let a caller point the server's include requests - and what ends up in the shared resource cache
 * - at any host it likes. Setups where the app is not reachable on loopback map their types explicitly via
 * {@code jsonapi4j.cd.mapping}.
 */
public class SelfFallbackRouting {

    /**
     * Set by the container only when the connection itself is TLS - unlike {@code getScheme()} and
     * {@code isSecure()}, which forwarded-header handling rewrites from {@code X-Forwarded-Proto}.
     */
    static final String CIPHER_SUITE_ATTRIBUTE = "jakarta.servlet.request.cipher_suite";

    /**
     * Literals rather than {@code localhost}, which may resolve to the other address family first and miss a server
     * that listens on only one of them.
     */
    static final String IPV4_LOOPBACK_ADDRESS = "127.0.0.1";
    static final String IPV6_LOOPBACK_ADDRESS = "[::1]";

    private final DomainSettingsResolver domainSettingsResolver;
    private final CompoundDocsProperties cdProperties;
    private final String rootPath;
    private final URI defaultMapping;

    /**
     * @param domainSettingsResolver the configured routing
     * @param cdProperties           supplies the batch size for same-app fetches
     * @param rootPath               the configured JSON:API root path
     */
    public SelfFallbackRouting(DomainSettingsResolver domainSettingsResolver,
                               CompoundDocsProperties cdProperties,
                               String rootPath) {
        this.domainSettingsResolver = domainSettingsResolver;
        this.cdProperties = cdProperties;
        this.rootPath = rootPath;
        this.defaultMapping = cdProperties.defaultMapping().map(URI::create).orElse(null);
    }

    /**
     * A {@code null} from the configured resolver breaks its contract and is passed on as is, so
     * {@link DomainSettingsResolver#requireDomainSettings(String)} reports it rather than it being taken for "no route".
     *
     * @return routing for {@code servletRequest}: the configured route when there is one, otherwise this app - at
     * {@code jsonapi4j.cd.mapping.default} when set, over loopback when not
     */
    public DomainSettingsResolver forRequest(HttpServletRequest servletRequest) {
        URI selfBaseUrl = defaultMapping != null ? defaultMapping : selfBaseUrl(servletRequest);
        return resourceType -> {
            Optional<DomainSettings> configured = domainSettingsResolver.resolveDomainSettings(resourceType);
            if (configured == null) {
                return null;
            }
            return configured.or(() -> Optional.of(new DomainSettings(selfBaseUrl, maxBatchSize(resourceType))));
        };
    }

    private URI selfBaseUrl(HttpServletRequest servletRequest) {
        String baseUrl = String.format(
                "%s://%s:%d",
                localScheme(servletRequest),
                loopbackAddress(servletRequest),
                servletRequest.getLocalPort()
        );
        return URI.create(BaseUrls.join(baseUrl + servletRequest.getContextPath(), rootPath));
    }

    private int maxBatchSize(String resourceType) {
        return cdProperties.batchSizeMapping().getOrDefault(resourceType, cdProperties.defaultMaxBatchSize());
    }

    private String localScheme(HttpServletRequest servletRequest) {
        return servletRequest.getAttribute(CIPHER_SUITE_ATTRIBUTE) != null ? "https" : "http";
    }

    /**
     * An IPv4-mapped IPv6 address ({@code ::ffff:10.0.0.5}) is an IPv4 connection accepted by a dual-stack socket, so it
     * gets the IPv4 loopback. Only an address containing {@code :} is parsed, and an IPv6 literal never triggers a DNS
     * lookup. A zone ({@code %eth0}) is dropped first: it says nothing about the family, and a zone naming an interface
     * that does not exist on this machine would fail to parse.
     */
    private String loopbackAddress(HttpServletRequest servletRequest) {
        String localAddress = servletRequest.getLocalAddr();
        if (localAddress == null || !localAddress.contains(":")) {
            return IPV4_LOOPBACK_ADDRESS;
        }
        try {
            return InetAddress.getByName(StringUtils.substringBefore(localAddress, "%")) instanceof Inet6Address
                    ? IPV6_LOOPBACK_ADDRESS
                    : IPV4_LOOPBACK_ADDRESS;
        } catch (UnknownHostException e) {
            return IPV4_LOOPBACK_ADDRESS;
        }
    }

}
