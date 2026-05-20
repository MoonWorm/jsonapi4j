package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDomainSettingsResolverTests {

    private static final URI USERS_URL = URI.create("http://users.example.com");
    private static final URI COUNTRIES_URL = URI.create("http://countries.example.com");

    @Test
    void resolveDomainSettings_knownType_returnsMappedUrl() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("users").url()).isEqualTo(USERS_URL);
        assertThat(resolver.resolveDomainSettings("countries").url()).isEqualTo(COUNTRIES_URL);
    }

    @Test
    void resolveDomainSettings_unknownType_returnsDefaultUrl() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        DomainSettings settings = resolver.resolveDomainSettings("unknown");

        assertThat(settings.url()).isEqualTo(URI.create("http://localhost:8080"));
    }

    @Test
    void resolveDomainSettings_noOverride_usesGlobalDefaultBatchSize() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        DomainSettings settings = resolver.resolveDomainSettings("users");

        assertThat(settings.maxBatchSize()).isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
    }

    @Test
    void resolveDomainSettings_perTypeOverride_usesOverride() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of("users", 50),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("users").maxBatchSize()).isEqualTo(50);
        assertThat(resolver.resolveDomainSettings("countries").maxBatchSize()).isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
    }

    @Test
    void resolveDomainSettings_customGlobalDefault_appliedWhenNoOverride() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                100
        );

        DomainSettings settings = resolver.resolveDomainSettings("users");

        assertThat(settings.maxBatchSize()).isEqualTo(100);
    }

    @Test
    void resolveDomainSettings_overrideTakesPrecedenceOverGlobalDefault() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of("countries", 5),
                100
        );

        assertThat(resolver.resolveDomainSettings("users").maxBatchSize()).isEqualTo(100);
        assertThat(resolver.resolveDomainSettings("countries").maxBatchSize()).isEqualTo(5);
    }

    @Test
    void resolveDomainSettings_unknownType_returnsDefaultUrlAndGlobalDefaultBatchSize() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of("users", 50),
                100
        );

        DomainSettings settings = resolver.resolveDomainSettings("unknown");

        assertThat(settings.url()).isEqualTo(URI.create("http://localhost:8080"));
        assertThat(settings.maxBatchSize()).isEqualTo(100);
    }
}
