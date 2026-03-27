package pro.api4.jsonapi4j.plugin.cd;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

/**
 * Marker plugin class - no implementation needed. Everything is controlled via Servlet Filter.
 */
@Slf4j
public class JsonApiCompoundDocsPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiCompoundDocsPlugin.class.getSimpleName();

    @Override
    public String pluginName() {
        return NAME;
    }

}