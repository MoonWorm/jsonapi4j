package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.config.exception.RootConfigMisconfigurationException;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApi4jBuilderTests {

    private final JsonApi4jBuilder sut = JsonApi4j.builder();

    @Test
    public void build_noPropertiesSet_buildsSuccessfully() {
        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_invalidRootProperties_throwsRootConfigMisconfigurationException() {
        sut.properties(rootPropertiesWithRootPath("jsonapi"));

        assertThatThrownBy(sut::build)
                .isInstanceOf(RootConfigMisconfigurationException.class)
                .hasMessageContaining("jsonapi4j.rootPath");
    }

    @Test
    public void build_rootAndPluginBothInvalid_reportsRootFirst() {
        sut.properties(rootPropertiesWithRootPath("jsonapi"))
                .pluginRegistry(PluginRegistry.builder()
                        .register(new TestPlugin("SfPlugin", new TestProperties("sf", true), true))
                        .build());

        assertThatThrownBy(sut::build).isInstanceOf(RootConfigMisconfigurationException.class);
    }

    @Test
    public void build_pluginCrossCheckFails_throwsPluginMisconfigurationException() {
        sut.pluginRegistry(PluginRegistry.builder()
                .register(new TestPlugin("SfPlugin", new CrossCheckingProperties("sf"), true))
                .build());

        assertThatThrownBy(sut::build)
                .isInstanceOf(PluginMisconfigurationException.class)
                .hasMessageContaining("does not agree with '/jsonapi'");
    }

    @Test
    public void properties_null_throwsNullPointerException() {
        assertThatThrownBy(() -> sut.properties(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void build_pluginExposesNoConfigProperties_buildsSuccessfully() {
        sut.pluginRegistry(PluginRegistry.builder().register(new TestPlugin("NoConfigPlugin", null, true)).build());

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_pluginWithValidConfig_buildsSuccessfully() {
        sut.pluginRegistry(PluginRegistry.builder()
                .register(new TestPlugin("ValidPlugin", new TestProperties("sf", false), true))
                .build());

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_pluginWithInvalidConfig_throwsPluginMisconfigurationException() {
        sut.pluginRegistry(PluginRegistry.builder()
                .register(new TestPlugin("SfPlugin", new TestProperties("sf", true), true))
                .build());

        assertThatThrownBy(sut::build)
                .isInstanceOf(PluginMisconfigurationException.class)
                .hasMessageContaining("SfPlugin ('jsonapi4j.sf')")
                .hasMessageContaining("jsonapi4j.sf.brokenProperty");
    }

    @Test
    public void build_disabledPluginWithInvalidConfig_buildsSuccessfully() {
        sut.pluginRegistry(PluginRegistry.builder()
                .register(new TestPlugin("SfPlugin", new TestProperties("sf", true), false))
                .build());

        assertThatCode(sut::build).doesNotThrowAnyException();
    }

    @Test
    public void build_severalPluginRegistryWithInvalidConfig_reportsAllOfThem() {
        sut.pluginRegistry(PluginRegistry.builder()
                .register(new TestPlugin("SfPlugin", new TestProperties("sf", true), true))
                .register(new TestPlugin("CdPlugin", new TestProperties("cd", true), true))
                .build());

        assertThatThrownBy(sut::build)
                .isInstanceOf(PluginMisconfigurationException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("SfPlugin", "CdPlugin"));
    }

    private static JsonApi4jProperties rootPropertiesWithRootPath(String rootPath) {
        DefaultJsonApi4jProperties properties = new DefaultJsonApi4jProperties();
        properties.setRootPath(rootPath);
        return properties;
    }

    private record CrossCheckingProperties(String section) implements PluginProperties {

        @Override
        public PropertiesValidationResult validateAgainst(JsonApi4jProperties rootProperties) {
            return PropertiesValidationResult.builder()
                    .addCrossPropertiesError(String.format(
                            "'%s' does not agree with '%s'", propertyPath("path"), rootProperties.rootPath()
                    ))
                    .build();
        }

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
        public PropertiesValidationResult validate() {
            if (!broken) {
                return PropertiesValidationResult.empty();
            }
            return PropertiesValidationResult.builder()
                    .requireNotBlank(propertyPath("brokenProperty"), null)
                    .build();
        }

    }

}
