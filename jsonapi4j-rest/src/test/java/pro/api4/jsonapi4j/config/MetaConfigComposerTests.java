package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetaConfigComposerTests {

    @Test
    void compose_rendersRootAtTopLevelAndTypedPluginSections() {
        DefaultJsonApi4jProperties root = new DefaultJsonApi4jProperties();
        root.setRootPath("/jsonapi");

        Map<String, Object> settings = MetaConfigComposer.compose(
                root,
                List.of(new TestPlugin(new TestCompoundDocsProperties(
                        true,
                        List.of("FIELDS", "CUSTOM_QUERY_PARAMS", "HEADERS")
                )))
        );

        assertThat(settings).containsEntry("rootPath", "/jsonapi");
        assertThat(settings).containsKey("cd");

        Map<String, Object> cd = asMap(settings.get("cd"));
        assertThat(cd.get("propagation")).isInstanceOf(List.class);
        assertThat(asList(cd.get("propagation")))
                .containsExactly("FIELDS", "CUSTOM_QUERY_PARAMS", "HEADERS");
        assertThat(cd).containsEntry("enabled", true);
        assertThat(cd).doesNotContainKey("section");
    }

    @Test
    void compose_skipsPluginsWithoutConfig() {
        Map<String, Object> settings = MetaConfigComposer.compose(
                new DefaultJsonApi4jProperties(),
                List.of(new TestPlugin(null))
        );

        assertThat(settings).doesNotContainKey("cd");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return (List<Object>) value;
    }

    private record TestCompoundDocsProperties(boolean enabled, List<String> propagation) implements PluginProperties {
        @Override
        public String section() {
            return "cd";
        }
    }

    private record TestPlugin(PluginProperties properties) implements JsonApi4jPlugin {
        @Override
        public String pluginName() {
            return "test";
        }

        @Override
        public PluginProperties configProperties() {
            return properties;
        }
    }

}
