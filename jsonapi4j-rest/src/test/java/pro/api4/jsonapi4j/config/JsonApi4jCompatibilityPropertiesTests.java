package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApi4jCompatibilityPropertiesTests {

    @Test
    public void compatibilitySettingsAbsent_defaultsToStrictMode() {
        // given - when
        JsonApi4jProperties sut = JsonApi4jConfigReader.convertToConfig(Map.of(), JsonApi4jProperties.class);

        // then
        assertThat(sut.getCompatibility().isLegacyMode()).isFalse();
        assertThat(sut.getCompatibility().resolveMode()).isEqualTo(JsonApi4jCompatibilityMode.STRICT);
    }

    @Test
    public void compatibilityLegacyModeEnabled_resolvesToLegacyMode() {
        // given - when
        JsonApi4jProperties sut = JsonApi4jConfigReader.convertToConfig(
                Map.of("compatibility", Map.of("legacyMode", true)),
                JsonApi4jProperties.class
        );

        // then
        assertThat(sut.getCompatibility().isLegacyMode()).isTrue();
        assertThat(sut.getCompatibility().resolveMode()).isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
    }
}
