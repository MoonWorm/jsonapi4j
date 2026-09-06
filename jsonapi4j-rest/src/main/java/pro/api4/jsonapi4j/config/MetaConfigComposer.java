package pro.api4.jsonapi4j.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Composes the effective {@code jsonapi4j.*} configuration subtree (for the {@code config} meta resource) from the
 * already-bound, strictly-typed config objects: the root {@link JsonApi4jProperties} at the top level plus each
 * plugin's {@link PluginProperties} keyed by its {@link PluginProperties#section()}. Serialization runs through a
 * single typed Jackson pass (no flat-key reconstruction), so list-valued properties render as JSON arrays and
 * effective defaults are always present, identically across host stacks.
 */
public final class MetaConfigComposer {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final ObjectMapper MAPPER = JsonApi4jConfigReader.getJsonObjectMapper()
            .copy()
            .addMixIn(ValidatableProperties.class, ValidatablePropertiesMixIn.class);

    private MetaConfigComposer() {
    }

    /**
     * @param root    the root jsonapi4j properties (e.g. {@code rootPath}, {@code validation}, {@code meta})
     * @param pluginRegistry the registered plugins - every one of them, enabled or not; each may contribute its
     *                {@link PluginProperties} via {@link JsonApi4jPlugin#configProperties()}
     * @return an ordered, nested map with the root config at the top level and every non-null plugin section keyed
     * by its {@link PluginProperties#section()}
     */
    public static Map<String, Object> compose(JsonApi4jProperties root,
                                              PluginRegistry pluginRegistry) {
        Map<String, Object> settings = new LinkedHashMap<>(toMap(root));
        for (JsonApi4jPlugin plugin : pluginRegistry.getAllPlugins()) {
            PluginProperties properties = plugin.configProperties();
            if (properties != null) {
                settings.put(properties.section(), toMap(properties));
            }
        }
        return settings;
    }

    private static Map<String, Object> toMap(Object config) {
        return MAPPER.convertValue(config, MAP_TYPE);
    }

    /**
     * Keeps the {@link ValidatableProperties} contract itself out of the emitted config: an implementation is free
     * to be a record, whose {@code section} / {@code propertyPathPrefix} components would otherwise surface as
     * configuration keys.
     */
    @JsonIgnoreProperties({"section", "propertyPathPrefix"})
    private interface ValidatablePropertiesMixIn {
    }

}
