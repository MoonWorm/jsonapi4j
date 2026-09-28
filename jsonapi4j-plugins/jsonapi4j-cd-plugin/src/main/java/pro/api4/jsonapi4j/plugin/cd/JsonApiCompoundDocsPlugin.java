package pro.api4.jsonapi4j.plugin.cd;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;
import pro.api4.jsonapi4j.util.ReflectionUtils;

/**
 * Marker plugin class - request processing is controlled via a Servlet Filter. It carries the effective
 * {@link CompoundDocsProperties} so the plugin can expose its configuration via {@link #configProperties()}.
 */
@Slf4j
public class JsonApiCompoundDocsPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiCompoundDocsPlugin.class.getSimpleName();

    private final CompoundDocsProperties compoundDocsProperties;

    public JsonApiCompoundDocsPlugin(CompoundDocsProperties compoundDocsProperties) {
        this.compoundDocsProperties = compoundDocsProperties;
    }

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        return compoundDocsProperties.enabled();
    }

    @Override
    public CompoundDocsProperties configProperties() {
        return compoundDocsProperties;
    }

    /**
     * Rejects a resource type named {@code default} whenever the plugin is enabled - that name is the reserved
     * {@code jsonapi4j.cd.mapping.default} key. Failing regardless of whether the key is set surfaces the clash in
     * development, instead of only once some deployment sets it (e.g. through an environment variable).
     */
    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        JsonApiResource jsonApiResource = ReflectionUtils.findAnnotationForClass(resource.getClass(), JsonApiResource.class);
        if (jsonApiResource != null && CompoundDocsProperties.DEFAULT_MAPPING_KEY.equals(jsonApiResource.resourceType())) {
            throw new PluginMisconfigurationException(String.format(
                    "Resource type '%s' (%s) clashes with the reserved key '%s' of %s - rename the resource type",
                    jsonApiResource.resourceType(),
                    resource.getClass().getName(),
                    compoundDocsProperties.propertyPath(CompoundDocsProperties.MAPPING_PROPERTY, CompoundDocsProperties.DEFAULT_MAPPING_KEY),
                    NAME
            ));
        }
        return null;
    }

}
