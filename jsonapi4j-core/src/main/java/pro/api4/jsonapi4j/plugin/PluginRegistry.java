package pro.api4.jsonapi4j.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PluginRegistry {

    /**
     * The order every consumer sees: ascending {@link JsonApi4jPlugin#precedence()}, ties broken by plugin name so
     * two hosts registering the same plugins run them in the same order.
     */
    private static final Comparator<JsonApi4jPlugin> CANONICAL_ORDER =
            Comparator.comparingInt(JsonApi4jPlugin::precedence).thenComparing(JsonApi4jPlugin::pluginName);

    private final List<JsonApi4jPlugin> plugins;
    private final List<JsonApi4jPlugin> activePlugins;
    private final Map<String, JsonApi4jPlugin> pluginsByName;

    private PluginRegistry(List<JsonApi4jPlugin> plugins) {
        this.plugins = plugins.stream().sorted(CANONICAL_ORDER).toList();
        this.activePlugins = this.plugins.stream().filter(JsonApi4jPlugin::enabled).toList();
        this.pluginsByName = this.plugins
                .stream().collect(CustomCollectors.toOrderedMap(JsonApi4jPlugin::pluginName, Function.identity()));
    }

    public static PluginRegistryBuilder builder() {
        return new PluginRegistryBuilder();
    }

    public static PluginRegistry empty() {
        return builder().build();
    }

    /**
     * @return every registered plugin, enabled or not, in canonical order. Introspection ({@code /plugins} meta
     * resource) and the composed {@code config} view read this: a disabled plugin still reports its state and
     * publishes its configuration.
     */
    public List<JsonApi4jPlugin> getAllPlugins() {
        return plugins;
    }

    /**
     * @return the enabled plugins only, in canonical order. Request processing and registration-time info
     * extraction read this, so a disabled plugin never reaches a visitor or a plugin-info map.
     */
    public List<JsonApi4jPlugin> getActivePlugins() {
        return activePlugins;
    }

    public Optional<JsonApi4jPlugin> getByName(String pluginName) {
        return Optional.ofNullable(pluginsByName.get(pluginName));
    }

    /**
     * Whether a plugin with this name is registered <em>and</em> enabled. Lets one plugin react to another without
     * depending on it - peers are matched by name, never by type.
     */
    public boolean isActivePlugin(String pluginName) {
        return getByName(pluginName).filter(JsonApi4jPlugin::enabled).isPresent();
    }

    /**
     * Looks up a plugin's bound configuration by its type, e.g. {@code configOf(OasProperties.class)}. Reads
     * {@link #getAllPlugins()}: configuration exists whether or not the plugin is enabled.
     *
     * @return the configuration, or empty when no registered plugin exposes one of that type
     */
    public <CONFIG extends PluginProperties> Optional<CONFIG> configOf(Class<CONFIG> configType) {
        Validate.notNull(configType, "Config type must not be null");
        return plugins.stream()
                .map(JsonApi4jPlugin::configProperties)
                .filter(configType::isInstance)
                .map(configType::cast)
                .findFirst();
    }

    /**
     * Validates every enabled plugin's configuration - on its own ({@link PluginProperties#validate()}) and against
     * the root configuration ({@link PluginProperties#validateAgainst(JsonApi4jProperties)}) - and fails with every
     * plugin's errors at once. A boot that dies on the first bad key costs one restart per typo.
     *
     * @param rootProperties the effective root configuration the plugins are checked against
     * @throws PluginMisconfigurationException when any enabled plugin is misconfigured
     */
    public void validateConfigs(JsonApi4jProperties rootProperties) {
        StringBuilder errors = new StringBuilder();
        activePlugins.stream()
                .filter(plugin -> plugin.configProperties() != null)
                .forEach(plugin -> {
                    PluginProperties pluginProperties = plugin.configProperties();
                    PropertiesValidationResult result = PropertiesValidationResult.builder()
                            .addAll(pluginProperties.validate())
                            .addAll(pluginProperties.validateAgainst(rootProperties))
                            .build();
                    if (result.hasErrors()) {
                        errors.append(MessageFormat.format(
                                "{0} (''{1}.{2}''):\n{3}",
                                plugin.pluginName(),
                                JsonApi4jProperties.CONFIG_PREFIX,
                                pluginProperties.section(),
                                result
                        ));
                    }
                });
        if (!errors.isEmpty()) {
            throw new PluginMisconfigurationException(
                    "Registered plugins are misconfigured. Fix the configuration and restart:\n" + errors
            );
        }
    }

    @Slf4j
    public static class PluginRegistryBuilder {

        private final List<JsonApi4jPlugin> plugins = new ArrayList<>();

        private PluginRegistryBuilder() {
        }

        public PluginRegistryBuilder registerAll(List<JsonApi4jPlugin> plugins) {
            Validate.notNull(plugins, "Plugins must not be null");
            plugins.forEach(this::register);
            return this;
        }

        public PluginRegistryBuilder register(JsonApi4jPlugin plugin) {
            Validate.notNull(plugin, "Plugin must not be null");
            this.plugins.add(plugin);
            log.info("{} plugin has been registered (enabled: {}).", plugin.pluginName(), plugin.enabled());
            return this;
        }

        public PluginRegistry build() {
            assertUniquePluginNames();
            assertUniqueConfigSections();
            return new PluginRegistry(plugins);
        }

        /**
         * A plugin name keys the info a plugin extracts at registration time and the id of its {@code plugins} meta
         * resource, so two plugins sharing one silently overwrite each other's info and collide in the meta API.
         */
        private void assertUniquePluginNames() {
            duplicatesOf(JsonApi4jPlugin::pluginName).forEach(name -> {
                throw new PluginMisconfigurationException(MessageFormat.format(
                        "Multiple plugins are registered under the same ({0}) name. Plugin names must be unique - " +
                                "they key the extracted plugin info and the ids of the meta API plugin resources.",
                        name
                ));
            });
        }

        /**
         * A config section keys the plugin's subtree in the composed {@code config} view, so two plugins claiming
         * one would publish only the last one's configuration.
         */
        private void assertUniqueConfigSections() {
            duplicatesOf(plugin -> plugin.configProperties() == null ? null : plugin.configProperties().section())
                    .forEach(section -> {
                        throw new PluginMisconfigurationException(MessageFormat.format(
                                "Multiple plugins are registered for the same (''{0}.{1}'') config section. Each " +
                                        "plugin must occupy its own subtree.",
                                JsonApi4jProperties.CONFIG_PREFIX,
                                section
                        ));
                    });
        }

        private Set<String> duplicatesOf(Function<JsonApi4jPlugin, String> key) {
            Set<String> seen = new HashSet<>();
            return plugins.stream()
                    .map(key)
                    .filter(Objects::nonNull)
                    .filter(value -> !seen.add(value))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

    }

}
