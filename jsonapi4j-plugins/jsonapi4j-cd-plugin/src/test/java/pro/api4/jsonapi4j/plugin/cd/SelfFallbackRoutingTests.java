package pro.api4.jsonapi4j.plugin.cd;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.Transport;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelfFallbackRoutingTests {

    private static final URI COUNTRIES_URL = URI.create("http://geo.internal/jsonapi");
    private static final URI APP_URL = URI.create("https://api.internal:8443/jsonapi");
    private static final DefaultDomainSettingsResolver COUNTRIES_ONLY = new DefaultDomainSettingsResolver(
            Map.of("countries", DomainSettings.overHttp(COUNTRIES_URL))
    );

    private final DefaultCompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();

    @Nested
    class SelfFallback {

        @Test
        void resolveDomainSettings_unmappedType_isFetchedInProcess() {
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users"))
                    .contains(DomainSettings.inProcess(DomainSettings.DEFAULT_MAX_BATCH_SIZE));
        }

        @Test
        void resolveDomainSettings_unmappedTypeWithBatchSize_isFetchedInProcessInBatchesOfIt() {
            cdProperties.setMapping(Map.of("users", mapping(null, 50)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users")).contains(DomainSettings.inProcess(50));
        }

        @Test
        void resolveDomainSettings_unmappedTypeWithDefaultBatchSize_isFetchedInProcessInBatchesOfIt() {
            cdProperties.setMapping(Map.of("default", mapping(null, 50)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users")).contains(DomainSettings.inProcess(50));
        }

        @Test
        void resolveDomainSettings_defaultUrlWithoutTransport_isFetchedInProcess() {
            cdProperties.setMapping(Map.of("default", transportMapping(APP_URL.toString(), null)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users"))
                    .contains(DomainSettings.inProcess(DomainSettings.DEFAULT_MAX_BATCH_SIZE));
        }

        @Test
        void resolveDomainSettings_defaultTransportHttp_isFetchedOverHttpFromDefaultUrlWithCredentials() {
            cdProperties.setMapping(Map.of("default", transportMapping(APP_URL.toString(), Transport.HTTP)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users"))
                    .contains(DomainSettings.overHttp(APP_URL, DomainSettings.DEFAULT_MAX_BATCH_SIZE, true));
        }

        @Test
        void resolveDomainSettings_typeInProcessUnderDefaultHttp_isFetchedInProcess() {
            cdProperties.setMapping(Map.of(
                    "default", transportMapping(APP_URL.toString(), Transport.HTTP),
                    "users", transportMapping(null, Transport.IN_PROCESS)
            ));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("users"))
                    .contains(DomainSettings.inProcess(DomainSettings.DEFAULT_MAX_BATCH_SIZE));
            assertThat(sut.resolveDomainSettings("currencies"))
                    .contains(DomainSettings.overHttp(APP_URL, DomainSettings.DEFAULT_MAX_BATCH_SIZE, true));
        }

        @Test
        void resolveDomainSettings_typeHttpWithoutDefaultUrl_throwsDomainResolution() {
            cdProperties.setMapping(Map.of("users", transportMapping(null, Transport.HTTP)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThatThrownBy(() -> sut.resolveDomainSettings("users"))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessageContaining("'mapping.default.url' is not set");
        }

    }

    @Nested
    class ConfiguredRoutes {

        @Test
        void resolveDomainSettings_mappedType_usesConfiguredRoute() {
            cdProperties.setMapping(Map.of("default", transportMapping(APP_URL.toString(), null)));
            SelfFallbackRouting sut = new SelfFallbackRouting(COUNTRIES_ONLY, cdProperties);

            assertThat(sut.resolveDomainSettings("countries")).contains(DomainSettings.overHttp(COUNTRIES_URL));
        }

        @Test
        void requireDomainSettings_configuredResolverReturnsNull_reportsBrokenContract() {
            SelfFallbackRouting sut = new SelfFallbackRouting(resourceType -> null, cdProperties);

            assertThatThrownBy(() -> sut.requireDomainSettings("users"))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessage("DomainSettingsResolver returned null instead of an Optional");
        }

    }

    private static DefaultCompoundDocsProperties.DefaultMapping mapping(String url, Integer maxBatchSize) {
        return new DefaultCompoundDocsProperties.DefaultMapping(url, maxBatchSize, false);
    }

    private static DefaultCompoundDocsProperties.DefaultMapping transportMapping(String url, Transport transport) {
        return new DefaultCompoundDocsProperties.DefaultMapping(url, null, false, transport);
    }

}
