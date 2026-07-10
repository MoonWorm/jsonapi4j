package pro.api4.jsonapi4j.plugin.cd;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;

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
    public CompoundDocsProperties configProperties() {
        return compoundDocsProperties;
    }

}
