package pro.api4.jsonapi4j.meta.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.config.Integration;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.context.MetaRuntime;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.PluginAttributes;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MetaIntrospectorTests {

    private final MetaIntrospector sut = new MetaIntrospector(new MetaRuntime(
            MetaContext.of(Map.of(), Integration.SERVLET),
            PluginRegistry.builder()
                    .register(new TestPlugin("EnabledPlugin", true))
                    .register(new TestPlugin("DisabledPlugin", false))
                    .build(),
            DomainRegistry.empty(),
            OperationsRegistry.empty()
    ));

    @Test
    public void plugins_disabledPlugin_isReportedAsDisabledRatherThanOmitted() {
        assertThat(sut.plugins())
                .extracting(PluginAttributes::name, PluginAttributes::enabled)
                .containsExactly(
                        tuple("DisabledPlugin", false),
                        tuple("EnabledPlugin", true)
                );
    }

    @Test
    public void pluginById_disabledPlugin_isStillAddressable() {
        assertThat(sut.pluginById("DisabledPlugin")).isPresent();
    }

    private record TestPlugin(String name, boolean enabled) implements JsonApi4jPlugin {

        @Override
        public String pluginName() {
            return name;
        }

    }

}
