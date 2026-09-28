package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class SelfFallbackRoutingTests {

    private static final URI COUNTRIES_URL = URI.create("http://geo.internal/jsonapi");

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final DefaultCompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
    private final SelfFallbackRouting sut = new SelfFallbackRouting(
            DefaultDomainSettingsResolver.from(Map.of("countries", COUNTRIES_URL.toString()), Map.of(), 20),
            cdProperties,
            "/jsonapi"
    );

    @BeforeEach
    public void setUp() {
        lenient().when(request.getHeader("Host")).thenReturn("evil.example:6666");
        lenient().when(request.getScheme()).thenReturn("https");
        lenient().when(request.getServerName()).thenReturn("evil.example");
        lenient().when(request.getServerPort()).thenReturn(6666);
        lenient().when(request.getLocalAddr()).thenReturn("10.0.0.5");
        lenient().when(request.getLocalPort()).thenReturn(8080);
        lenient().when(request.getContextPath()).thenReturn("/ctx");
    }

    @Nested
    class SelfFallback {

        @Test
        public void forRequest_clientControlledHostHeaders_usesLoopbackAndLocalPort() {
            URI selfBaseUrl = selfBaseUrl(sut);

            assertThat(selfBaseUrl).isEqualTo(URI.create("http://127.0.0.1:8080/ctx/jsonapi"));
        }

        @Test
        public void forRequest_tlsConnection_usesHttps() {
            lenient().when(request.getAttribute(SelfFallbackRouting.CIPHER_SUITE_ATTRIBUTE))
                    .thenReturn("TLS_AES_128_GCM_SHA256");

            assertThat(selfBaseUrl(sut)).isEqualTo(URI.create("https://127.0.0.1:8080/ctx/jsonapi"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0:0:0:0:0:0:0:1", "2001:db8:0:0:0:0:0:5", "fe80:0:0:0:0:0:0:1%eth0"})
        public void forRequest_requestArrivedOverIpv6_usesIpv6Loopback(String localAddress) {
            lenient().when(request.getLocalAddr()).thenReturn(localAddress);

            assertThat(selfBaseUrl(sut)).isEqualTo(URI.create("http://[::1]:8080/ctx/jsonapi"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"127.0.0.1", "10.0.0.5", "::ffff:10.0.0.5"})
        public void forRequest_requestArrivedOverIpv4_usesIpv4Loopback(String localAddress) {
            lenient().when(request.getLocalAddr()).thenReturn(localAddress);

            assertThat(selfBaseUrl(sut)).isEqualTo(URI.create("http://127.0.0.1:8080/ctx/jsonapi"));
        }

    }

    @Nested
    class DefaultMapping {

        @Test
        public void forRequest_defaultMappingSet_usesItInsteadOfLoopback() {
            cdProperties.setMapping(Map.of("default", "https://api.internal:8443/jsonapi"));
            SelfFallbackRouting routing = new SelfFallbackRouting(
                    DefaultDomainSettingsResolver.from(Map.of("countries", COUNTRIES_URL.toString()), Map.of(), 20),
                    cdProperties,
                    "/jsonapi"
            );

            assertThat(selfBaseUrl(routing)).isEqualTo(URI.create("https://api.internal:8443/jsonapi"));
        }

        @Test
        public void forRequest_defaultMappingSet_mappedTypeKeepsItsRoute() {
            cdProperties.setMapping(Map.of("default", "https://api.internal:8443/jsonapi"));
            SelfFallbackRouting routing = new SelfFallbackRouting(
                    DefaultDomainSettingsResolver.from(Map.of("countries", COUNTRIES_URL.toString()), Map.of(), 20),
                    cdProperties,
                    "/jsonapi"
            );

            assertThat(routing.forRequest(request).resolveDomainSettings("countries").orElseThrow().url())
                    .isEqualTo(COUNTRIES_URL);
        }

    }

    @Nested
    class Routing {

        @Test
        public void forRequest_mappedType_usesConfiguredRoute() {
            DomainSettings settings = domainSettings("countries");

            assertThat(settings.url()).isEqualTo(COUNTRIES_URL);
        }

        @Test
        public void forRequest_unmappedType_usesConfiguredBatchSize() {
            cdProperties.setBatchSizeMapping(Map.of("users", 50));

            assertThat(domainSettings("users").maxBatchSize()).isEqualTo(50);
            assertThat(domainSettings("state").maxBatchSize()).isEqualTo(cdProperties.defaultMaxBatchSize());
        }

    }

    private URI selfBaseUrl(SelfFallbackRouting routing) {
        return routing.forRequest(request)
                .resolveDomainSettings("users")
                .orElseThrow()
                .url();
    }

    private DomainSettings domainSettings(String resourceType) {
        return sut.forRequest(request)
                .resolveDomainSettings(resourceType)
                .orElseThrow();
    }

}
