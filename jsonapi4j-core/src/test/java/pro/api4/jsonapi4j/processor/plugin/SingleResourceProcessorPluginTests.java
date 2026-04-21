package pro.api4.jsonapi4j.processor.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.*;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SingleResourceProcessorPluginTests {

    private static final ResourceType SILVER = new ResourceType("silver");
    private static final String ID = "1";
    private static final String NAME = "test";
    private static final Dto DTO = new Dto(ID, NAME);
    private static final Request REQUEST = new Request(ID);

    @Mock
    private SingleDataItemSupplier<Request, Dto> ds;

    // --- DO_NOTHING ---

    @Test
    void plugin_doNothing_pipelineRunsNormally() {
        // given
        when(ds.get(REQUEST)).thenReturn(DTO);
        JsonApi4jPlugin plugin = doNothingPlugin();

        // when
        SingleResourceDoc<?> result = buildProcessor(plugin)
                .toSingleResourceDoc();

        // then
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(ID);
        verify(ds, times(1)).get(REQUEST);
    }

    // --- RETURN_DOC at pre-retrieval ---

    @Test
    void plugin_returnDocAtPreRetrieval_shortCircuitsBeforeDataFetch() {
        // given
        SingleResourceDoc<?> earlyDoc = new SingleResourceDoc<>(
                new ResourceObject<>("early", null, "silver", null, null, null, null),
                null, null
        );
        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public SingleResourceVisitors singleResourceVisitors() {
                return new SingleResourceVisitors() {
                    @Override
                    public <REQUEST> SingleResourceVisitors.DataPreRetrievalPhase<?> onDataPreRetrieval(
                            REQUEST request, OperationMeta operationMeta,
                            SingleResourceJsonApiContext<REQUEST, ?, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return SingleResourceVisitors.DataPreRetrievalPhase.returnDoc(earlyDoc);
                    }
                };
            }
        };

        // when
        SingleResourceDoc<?> result = buildProcessor(plugin)
                .toSingleResourceDoc();

        // then
        assertThat(result).isSameAs(earlyDoc);
        verify(ds, never()).get(any()); // data supplier should NOT be called
    }

    // --- MUTATE_REQUEST at pre-retrieval ---

    @Test
    void plugin_mutateRequestAtPreRetrieval_dataSupplierReceivesMutatedRequest() {
        // given
        Request mutatedRequest = new Request("mutated-id");
        when(ds.get(mutatedRequest)).thenReturn(DTO);

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public SingleResourceVisitors singleResourceVisitors() {
                return new SingleResourceVisitors() {
                    @Override
                    public <REQ> SingleResourceVisitors.DataPreRetrievalPhase<?> onDataPreRetrieval(
                            REQ request, OperationMeta operationMeta,
                            SingleResourceJsonApiContext<REQ, ?, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return SingleResourceVisitors.DataPreRetrievalPhase.mutatedRequest(mutatedRequest);
                    }
                };
            }
        };

        // when
        SingleResourceDoc<?> result = buildProcessor(plugin)
                .toSingleResourceDoc();

        // then
        assertThat(result.getData()).isNotNull();
        verify(ds, never()).get(REQUEST); // original request NOT used
        verify(ds, times(1)).get(mutatedRequest); // mutated request used
    }

    // --- RETURN_DOC at relationships pre-retrieval ---

    @Test
    void plugin_returnDocAtRelationshipsPreRetrieval_shortCircuitsBeforeRelationshipResolution() {
        // given
        when(ds.get(REQUEST)).thenReturn(DTO);
        SingleResourceDoc<?> overrideDoc = new SingleResourceDoc<>(
                new ResourceObject<>("override", null, "silver", null, null, null, null),
                LinksObject.builder().self("/override").build(), null
        );

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public SingleResourceVisitors singleResourceVisitors() {
                return new SingleResourceVisitors() {
                    @Override
                    public <REQ, DST, DOC extends SingleResourceDoc<?>>
                    SingleResourceVisitors.RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
                            REQ request, OperationMeta operationMeta, DST dataSourceDto,
                            DOC doc, SingleResourceJsonApiContext<REQ, DST, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return SingleResourceVisitors.RelationshipsPreRetrievalPhase.returnDoc(overrideDoc);
                    }
                };
            }
        };

        // when
        SingleResourceDoc<?> result = buildProcessor(plugin)
                .toSingleResourceDoc();

        // then — data was fetched, but the returned doc is the override
        assertThat(result).isSameAs(overrideDoc);
        verify(ds, times(1)).get(REQUEST); // data IS fetched
    }

    // --- MUTATE_DOC at relationships post-retrieval ---

    @Test
    void plugin_mutateDocAtRelationshipsPostRetrieval_returnsModifiedDoc() {
        // given
        when(ds.get(REQUEST)).thenReturn(DTO);
        SingleResourceDoc<?> mutatedDoc = new SingleResourceDoc<>(
                new ResourceObject<>(ID, null, "silver", null, null,
                        LinksObject.builder().self("/mutated").build(), null),
                null, "mutated-meta"
        );

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public SingleResourceVisitors singleResourceVisitors() {
                return new SingleResourceVisitors() {
                    @Override
                    public <REQ, DST, DOC extends SingleResourceDoc<?>>
                    SingleResourceVisitors.RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
                            REQ request, OperationMeta operationMeta, DST dataSourceDto,
                            DOC doc, SingleResourceJsonApiContext<REQ, DST, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return SingleResourceVisitors.RelationshipsPostRetrievalPhase.mutatedDoc(mutatedDoc);
                    }
                };
            }
        };

        // when
        SingleResourceDoc<?> result = buildProcessor(plugin)
                .toSingleResourceDoc();

        // then
        assertThat(result).isSameAs(mutatedDoc);
        assertThat(result.getMeta()).isEqualTo("mutated-meta");
    }

    // --- Helpers ---

    private pro.api4.jsonapi4j.processor.single.resource.SingleResourceTerminalStage<Request, Dto, ?> buildProcessor(JsonApi4jPlugin plugin) {
        PluginSettings settings = PluginSettings.builder()
                .plugin(plugin)
                .operationMeta(null)
                .info(new JsonApiPluginInfo(null, null, null))
                .build();

        return new SingleResourceProcessor()
                .forRequest(REQUEST)
                .plugins(List.of(settings))
                .dataSupplier(ds)
                .attributesResolver(dto -> new Attributes(dto.getName()))
                .resourceTypeAndIdResolver(dto -> new IdAndType(dto.getId(), SILVER));
    }

    private JsonApi4jPlugin doNothingPlugin() {
        return new TestPlugin();
    }

    private static class TestPlugin implements JsonApi4jPlugin {
        @Override
        public String pluginName() {
            return "test-plugin";
        }
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    static class Request implements IncludeAwareRequest {
        private final String id;
        private List<String> effectiveIncludes;
    }

    @Data
    static class Dto {
        private final String id;
        private final String name;
    }

    @Data
    static class Attributes {
        private final String name;
    }

}
