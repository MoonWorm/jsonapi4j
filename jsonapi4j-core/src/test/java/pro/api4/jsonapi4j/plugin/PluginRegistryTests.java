package pro.api4.jsonapi4j.plugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PluginRegistryTests {

    @Nested
    class Ordering {

        @Test
        public void all_pluginsWithDifferentPrecedence_ordersByPrecedence() {
            PluginRegistry sut = PluginRegistry.builder()
                    .register(plugin("Lowest", JsonApi4jPlugin.LOWEST_PRECEDENCE))
                    .register(plugin("Highest", JsonApi4jPlugin.HIGHEST_PRECEDENCE))
                    .register(plugin("Low", JsonApi4jPlugin.LOW_PRECEDENCE))
                    .build();

            assertThat(sut.getAllPlugins()).extracting(JsonApi4jPlugin::pluginName)
                    .containsExactly("Highest", "Low", "Lowest");
        }

        @Test
        public void all_pluginsWithEqualPrecedence_ordersByName() {
            PluginRegistry sut = PluginRegistry.builder()
                    .register(plugin("Charlie", JsonApi4jPlugin.LOW_PRECEDENCE))
                    .register(plugin("Alpha", JsonApi4jPlugin.LOW_PRECEDENCE))
                    .register(plugin("Bravo", JsonApi4jPlugin.LOW_PRECEDENCE))
                    .build();

            assertThat(sut.getAllPlugins()).extracting(JsonApi4jPlugin::pluginName)
                    .containsExactly("Alpha", "Bravo", "Charlie");
        }

        @Test
        public void active_always_keepsTheSameOrderAsAll() {
            PluginRegistry sut = PluginRegistry.builder()
                    .register(plugin("Late", JsonApi4jPlugin.LOWEST_PRECEDENCE))
                    .register(plugin("Early", JsonApi4jPlugin.HIGHEST_PRECEDENCE))
                    .build();

            assertThat(sut.getActivePlugins()).isEqualTo(sut.getAllPlugins());
        }

    }

    @Nested
    class EnabledFiltering {

        private final PluginRegistry sut = PluginRegistry.builder()
                .register(enabledPlugin("EnabledPlugin"))
                .register(disabledPlugin("DisabledPlugin"))
                .build();

        @Test
        public void all_disabledPlugin_isIncluded() {
            assertThat(sut.getAllPlugins()).extracting(JsonApi4jPlugin::pluginName)
                    .containsExactly("DisabledPlugin", "EnabledPlugin");
        }

        @Test
        public void active_disabledPlugin_isExcluded() {
            assertThat(sut.getActivePlugins()).extracting(JsonApi4jPlugin::pluginName)
                    .containsExactly("EnabledPlugin");
        }

        @Test
        public void isActive_enabledPlugin_returnsTrue() {
            assertThat(sut.isActivePlugin("EnabledPlugin")).isTrue();
        }

        @Test
        public void isActive_disabledPlugin_returnsFalse() {
            assertThat(sut.isActivePlugin("DisabledPlugin")).isFalse();
        }

        @Test
        public void isActive_unknownPlugin_returnsFalse() {
            assertThat(sut.isActivePlugin("NoSuchPlugin")).isFalse();
        }

        @Test
        public void byName_disabledPlugin_isStillFound() {
            assertThat(sut.getByName("DisabledPlugin")).isPresent();
        }

        @Test
        public void byName_unknownPlugin_returnsEmpty() {
            assertThat(sut.getByName("NoSuchPlugin")).isEmpty();
        }

    }

    @Nested
    class ConfigLookup {

        @Test
        public void configOf_registeredConfigType_returnsIt() {
            TestProperties properties = new TestProperties("sf");
            PluginRegistry sut = PluginRegistry.builder().register(pluginWithConfig("SfPlugin", properties)).build();

            assertThat(sut.configOf(TestProperties.class)).contains(properties);
        }

        @Test
        public void configOf_disabledPlugin_stillReturnsItsConfig() {
            TestProperties properties = new TestProperties("sf");
            PluginRegistry sut = PluginRegistry.builder()
                    .register(new TestPlugin("SfPlugin", JsonApi4jPlugin.LOW_PRECEDENCE, false, properties))
                    .build();

            assertThat(sut.configOf(TestProperties.class)).contains(properties);
        }

        @Test
        public void configOf_pluginWithoutConfig_returnsEmpty() {
            PluginRegistry sut = PluginRegistry.builder().register(enabledPlugin("NoConfigPlugin")).build();

            assertThat(sut.configOf(TestProperties.class)).isEmpty();
        }

    }

    @Nested
    class Registration {

        @Test
        public void empty_always_hasNoPlugins() {
            assertThat(PluginRegistry.empty().getAllPlugins()).isEmpty();
            assertThat(PluginRegistry.empty().getActivePlugins()).isEmpty();
        }

        @Test
        public void registerAll_listOfPlugins_registersAllOfThem() {
            PluginRegistry sut = PluginRegistry.builder()
                    .registerAll(List.of(enabledPlugin("One"), enabledPlugin("Two")))
                    .build();

            assertThat(sut.getAllPlugins()).hasSize(2);
        }

        @Test
        public void all_always_isUnmodifiable() {
            PluginRegistry sut = PluginRegistry.builder().register(enabledPlugin("One")).build();

            assertThatThrownBy(() -> sut.getAllPlugins().add(enabledPlugin("Two")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        public void build_duplicatePluginName_throwsPluginMisconfigurationException() {
            assertThatThrownBy(() -> PluginRegistry.builder()
                    .register(enabledPlugin("SamePlugin"))
                    .register(enabledPlugin("SamePlugin"))
                    .build())
                    .isInstanceOf(PluginMisconfigurationException.class)
                    .hasMessageContaining("(SamePlugin)");
        }

        @Test
        public void build_duplicateConfigSection_throwsPluginMisconfigurationException() {
            assertThatThrownBy(() -> PluginRegistry.builder()
                    .register(pluginWithConfig("FirstPlugin", new TestProperties("sf")))
                    .register(pluginWithConfig("SecondPlugin", new TestProperties("sf")))
                    .build())
                    .isInstanceOf(PluginMisconfigurationException.class)
                    .hasMessageContaining("'jsonapi4j.sf'");
        }

        @Test
        public void build_pluginsWithoutConfig_doesNotReportDuplicateSections() {
            PluginRegistry sut = PluginRegistry.builder()
                    .register(enabledPlugin("One"))
                    .register(enabledPlugin("Two"))
                    .build();

            assertThat(sut.getAllPlugins()).hasSize(2);
        }

    }

    private static JsonApi4jPlugin plugin(String name, int precedence) {
        return new TestPlugin(name, precedence, true, null);
    }

    private static JsonApi4jPlugin enabledPlugin(String name) {
        return new TestPlugin(name, JsonApi4jPlugin.LOW_PRECEDENCE, true, null);
    }

    private static JsonApi4jPlugin disabledPlugin(String name) {
        return new TestPlugin(name, JsonApi4jPlugin.LOW_PRECEDENCE, false, null);
    }

    private static JsonApi4jPlugin pluginWithConfig(String name, PluginProperties properties) {
        return new TestPlugin(name, JsonApi4jPlugin.LOW_PRECEDENCE, true, properties);
    }

    private record TestPlugin(String name,
                              int pluginPrecedence,
                              boolean enabled,
                              PluginProperties properties) implements JsonApi4jPlugin {

        @Override
        public String pluginName() {
            return name;
        }

        @Override
        public int precedence() {
            return pluginPrecedence;
        }

        @Override
        public PluginProperties configProperties() {
            return properties;
        }

    }

    private record TestProperties(String section) implements PluginProperties {
    }

}
