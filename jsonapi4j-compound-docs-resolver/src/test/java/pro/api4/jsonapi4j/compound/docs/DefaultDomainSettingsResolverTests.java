package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDomainSettingsResolverTests {

    private static final DomainSettings USERS = new DomainSettings(URI.create("http://users.example.com"), 50, true);
    private static final DomainSettings COUNTRIES = DomainSettings.of(URI.create("http://countries.example.com"));

    private final DefaultDomainSettingsResolver sut = new DefaultDomainSettingsResolver(Map.of(
            "users", USERS,
            "countries", COUNTRIES
    ));

    @Nested
    class ResolveDomainSettings {

        @Test
        void resolveDomainSettings_mappedType_returnsItsSettings() {
            assertThat(sut.resolveDomainSettings("users")).contains(USERS);
            assertThat(sut.resolveDomainSettings("countries")).contains(COUNTRIES);
        }

        @Test
        void resolveDomainSettings_unmappedType_returnsEmpty() {
            assertThat(sut.resolveDomainSettings("currencies")).isEmpty();
        }

    }

}
