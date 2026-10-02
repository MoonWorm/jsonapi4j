package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainSettingsResolverTests {

    private static final DomainSettings SETTINGS = DomainSettings.overHttp(URI.create("http://example.com"));

    @Nested
    class RequireDomainSettings {

        @Test
        void requireDomainSettings_resolved_returnsSettings() {
            DomainSettingsResolver sut = resourceType -> Optional.of(SETTINGS);

            assertThat(sut.requireDomainSettings("users")).isEqualTo(SETTINGS);
        }

        @Test
        void requireDomainSettings_empty_throwsNoMapping() {
            DomainSettingsResolver sut = resourceType -> Optional.empty();

            assertThatThrownBy(() -> sut.requireDomainSettings("users"))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessage("Resource type 'users' has no mapping");
        }

        @Test
        void requireDomainSettings_null_throwsDomainResolutionException() {
            DomainSettingsResolver sut = resourceType -> null;

            assertThatThrownBy(() -> sut.requireDomainSettings("users"))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessage("DomainSettingsResolver returned null instead of an Optional");
        }

        @Test
        void requireDomainSettings_resolverThrows_wrapsCause() {
            IllegalStateException failure = new IllegalStateException("boom");
            DomainSettingsResolver sut = resourceType -> {
                throw failure;
            };

            assertThatThrownBy(() -> sut.requireDomainSettings("users"))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessage("Error resolving domain settings for resource type 'users'")
                    .hasCause(failure);
        }

    }

}
