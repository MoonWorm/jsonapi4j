package pro.api4.jsonapi4j.plugin.sf.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode.RETURN_ALL_FIELDS;
import static pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode.SPARSE_ALL_FIELDS;

public class DefaultSfPropertiesTests {

    @Test
    public void toSfProperties_noSfKey_returnsDefaults() {
        // given
        Map<String, Object> properties = Map.of();

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isTrue();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(SPARSE_ALL_FIELDS);
    }

    @Test
    public void toSfProperties_sfKeyNotAMap_returnsDefaults() {
        // given
        Map<String, Object> properties = Map.of("sf", "invalid");

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isTrue();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(SPARSE_ALL_FIELDS);
    }

    @Test
    public void toSfProperties_fullConfig_returnsConfiguredValues() {
        // given
        Map<String, Object> properties = Map.of(
                "sf", Map.of(
                        "enabled", false,
                        "requestedFieldsDontExistMode", "RETURN_ALL_FIELDS"
                )
        );

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isFalse();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(RETURN_ALL_FIELDS);
    }

    @Test
    public void toSfProperties_onlyEnabled_modeUsesDefault() {
        // given
        Map<String, Object> properties = Map.of("sf", Map.of("enabled", false));

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isFalse();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(SPARSE_ALL_FIELDS);
    }

    @Test
    public void toSfProperties_onlyMode_enabledUsesDefault() {
        // given
        Map<String, Object> properties = Map.of(
                "sf", Map.of("requestedFieldsDontExistMode", "RETURN_ALL_FIELDS")
        );

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isTrue();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(RETURN_ALL_FIELDS);
    }

    @Test
    public void toSfProperties_invalidModeString_throwsException() {
        // given
        Map<String, Object> properties = Map.of(
                "sf", Map.of("requestedFieldsDontExistMode", "INVALID")
        );

        // when/then
        assertThatThrownBy(() -> DefaultSfProperties.toSfProperties(properties))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void toSfProperties_enabledAsNonBoolean_usesDefault() {
        // given
        Map<String, Object> properties = Map.of("sf", Map.of("enabled", "true"));

        // when
        SfProperties result = DefaultSfProperties.toSfProperties(properties);

        // then
        assertThat(result.enabled()).isTrue();
        assertThat(result.requestedFieldsDontExistMode()).isEqualTo(SPARSE_ALL_FIELDS);
    }

}
