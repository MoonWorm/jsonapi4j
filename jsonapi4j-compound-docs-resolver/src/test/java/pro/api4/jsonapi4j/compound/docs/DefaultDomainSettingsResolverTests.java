package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDomainSettingsResolverTests {

    private static final URI USERS_URL = URI.create("http://users.example.com");
    private static final URI COUNTRIES_URL = URI.create("http://countries.example.com");
    private static final String SELF_BASE_URL = "https://host:8443/ctx/jsonapi";

    @Test
    void resolveDomainSettings_mappedType_returnsMappedUrl() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("users", SELF_BASE_URL).url()).isEqualTo(USERS_URL);
        assertThat(resolver.resolveDomainSettings("countries", SELF_BASE_URL).url()).isEqualTo(COUNTRIES_URL);
    }

    @Test
    void resolveDomainSettings_unmappedType_usesSelfBaseUrl() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("state", SELF_BASE_URL).url())
                .isEqualTo(URI.create(SELF_BASE_URL));
    }

    @Test
    void resolveDomainSettings_unmappedType_nullSelfBaseUrl_failsFast() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThatThrownBy(() -> resolver.resolveDomainSettings("state", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveDomainSettings_noOverride_usesGlobalDefaultBatchSize() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("users", null).maxBatchSize())
                .isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
    }

    @Test
    void resolveDomainSettings_perTypeOverride_usesOverride() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of("users", 50),
                DomainSettings.DEFAULT_MAX_BATCH_SIZE
        );

        assertThat(resolver.resolveDomainSettings("users", null).maxBatchSize()).isEqualTo(50);
        assertThat(resolver.resolveDomainSettings("countries", null).maxBatchSize())
                .isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
    }

    @Test
    void resolveDomainSettings_customGlobalDefault_appliedWhenNoOverride() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of(),
                100
        );

        assertThat(resolver.resolveDomainSettings("users", null).maxBatchSize()).isEqualTo(100);
    }

    @Test
    void resolveDomainSettings_overrideTakesPrecedenceOverGlobalDefault() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString(),
                        "countries", COUNTRIES_URL.toString()),
                Map.of("countries", 5),
                100
        );

        assertThat(resolver.resolveDomainSettings("users", null).maxBatchSize()).isEqualTo(100);
        assertThat(resolver.resolveDomainSettings("countries", null).maxBatchSize()).isEqualTo(5);
    }

    @Test
    void resolveDomainSettings_unmappedType_usesSelfBaseUrlAndGlobalDefaultBatchSize() {
        DomainSettingsResolver resolver = DefaultDomainSettingsResolver.from(
                Map.of("users", USERS_URL.toString()),
                Map.of("users", 50),
                100
        );

        DomainSettings settings = resolver.resolveDomainSettings("state", SELF_BASE_URL);

        assertThat(settings.url()).isEqualTo(URI.create(SELF_BASE_URL));
        assertThat(settings.maxBatchSize()).isEqualTo(100);
    }
}
