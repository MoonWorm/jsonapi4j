package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors.DataPostRetrievalPhase;
import pro.api4.jsonapi4j.plugin.context.SingleResourceVisitorContext;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the contract the terminal stages stopped enforcing themselves once the plugin registry took over the
 * enabled/disabled filtering: a disabled plugin's visitors must never run.
 */
public class JsonApi4jPluginDispatchTests {

    private static final ResourceType THINGS = new ResourceType("things");

    private final RecordingPlugin enabledPlugin = new RecordingPlugin("EnabledPlugin", true);
    private final RecordingPlugin disabledPlugin = new RecordingPlugin("DisabledPlugin", false);

    @Test
    public void execute_disabledPlugin_neverInvokesItsVisitors() {
        // given
        JsonApi4j sut = jsonApi4jWith(enabledPlugin, disabledPlugin);

        // when
        sut.execute(readById("42"));

        // then
        assertThat(enabledPlugin.invoked).isTrue();
        assertThat(disabledPlugin.invoked).isFalse();
    }

    private static JsonApi4j jsonApi4jWith(JsonApi4jPlugin... plugins) {
        PluginRegistry pluginRegistry = PluginRegistry.builder().registerAll(List.of(plugins)).build();
        return JsonApi4j.builder()
                .pluginRegistry(pluginRegistry)
                .domainRegistry(DomainRegistry.builder(pluginRegistry).resource(new ThingResource()).build())
                .operationsRegistry(OperationsRegistry.builder(pluginRegistry).operations(new ThingOperations()).build())
                .build();
    }

    private static JsonApiRequest readById(String id) {
        return DefaultJsonApiRequest.builder()
                .targetResourceType(THINGS)
                .operationType(OperationType.READ_RESOURCE_BY_ID)
                .resourceId(id)
                .build();
    }

    private static class RecordingPlugin implements JsonApi4jPlugin {

        private final String name;
        private final boolean enabled;
        private boolean invoked;

        private RecordingPlugin(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        @Override
        public String pluginName() {
            return name;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public SingleResourceVisitors singleResourceVisitors() {
            return new SingleResourceVisitors() {
                @Override
                public <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> DataPostRetrievalPhase<?> onDataPostRetrieval(
                        SingleResourceVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx) {
                    invoked = true;
                    return DataPostRetrievalPhase.doNothing();
                }
            };
        }

    }

    @JsonApiResource(resourceType = "things")
    private static class ThingResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dto) {
            return dto;
        }

        @Override
        public Object resolveAttributes(String dto) {
            return dto;
        }

    }

    @JsonApiResourceOperation(resource = ThingResource.class)
    private static class ThingOperations implements ResourceOperations<String> {

        @Override
        public String readById(JsonApiRequest request) {
            return request.getResourceId();
        }

    }

}
