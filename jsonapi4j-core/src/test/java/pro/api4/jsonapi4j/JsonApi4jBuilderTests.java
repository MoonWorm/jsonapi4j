package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApi4jBuilderTests {

    private final JsonApi4jBuilder sut = JsonApi4j.builder();

    @Test
    public void build_pluginExposesNoConfigProperties_buildsSuccessfully() {
        sut.plugins(List.of(new TestPlugin("NoConfigPlugin", null, true)));

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_pluginWithValidConfig_buildsSuccessfully() {
        sut.plugins(List.of(new TestPlugin("ValidPlugin", new TestProperties("sf", false), true)));

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_pluginWithInvalidConfig_throwsPluginMisconfigurationException() {
        sut.plugins(List.of(new TestPlugin("SfPlugin", new TestProperties("sf", true), true)));

        assertThatThrownBy(sut::build)
                .isInstanceOf(PluginMisconfigurationException.class)
                .hasMessageContaining("SfPlugin ('jsonapi4j.sf')")
                .hasMessageContaining("jsonapi4j.sf.brokenProperty");
    }

    @Test
    public void build_disabledPluginWithInvalidConfig_buildsSuccessfully() {
        sut.plugins(List.of(new TestPlugin("SfPlugin", new TestProperties("sf", true), false)));

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_severalPluginsWithInvalidConfig_reportsAllOfThem() {
        sut.plugins(List.of(
                new TestPlugin("SfPlugin", new TestProperties("sf", true), true),
                new TestPlugin("CdPlugin", new TestProperties("cd", true), true)
        ));

        assertThatThrownBy(sut::build)
                .isInstanceOf(PluginMisconfigurationException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("SfPlugin", "CdPlugin"));
    }

    private record TestPlugin(String name,
                              PluginProperties properties,
                              boolean enabled) implements JsonApi4jPlugin {

        @Override
        public String pluginName() {
            return name;
        }

        @Override
        public PluginProperties configProperties() {
            return properties;
        }

    }

    private record TestProperties(String section, boolean broken) implements PluginProperties {

        @Override
        public PluginPropertiesValidationResult validate() {
            if (!broken) {
                return PluginPropertiesValidationResult.empty();
            }
            return PluginPropertiesValidationResult.builder()
                    .requireNotBlank(propertyPath("brokenProperty"), null)
                    .build();
        }

    }

}
