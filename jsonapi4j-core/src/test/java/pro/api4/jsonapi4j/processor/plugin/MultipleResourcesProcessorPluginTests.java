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
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.*;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesProcessor;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipleResourcesProcessorPluginTests {

    private static final ResourceType SILVER = new ResourceType("silver");
    private static final Dto DTO_1 = new Dto("1", "first");
    private static final Dto DTO_2 = new Dto("2", "second");
    private static final Request REQUEST = new Request();

    @Mock
    private MultipleDataItemsSupplier<Request, Dto> ds;

    // --- DO_NOTHING ---

    @Test
    void plugin_doNothing_pipelineRunsNormally() {
        // given
        when(ds.get(REQUEST)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1, DTO_2)));
        JsonApi4jPlugin plugin = doNothingPlugin();

        // when
        MultipleResourcesDoc<?> result = buildProcessor(plugin)
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).hasSize(2);
        verify(ds, times(1)).get(REQUEST);
    }

    // --- RETURN_DOC at pre-retrieval ---

    @Test
    void plugin_returnDocAtPreRetrieval_shortCircuitsBeforeDataFetch() {
        // given
        MultipleResourcesDoc<?> earlyDoc = new MultipleResourcesDoc<>(
                Collections.emptyList(), LinksObject.builder().self("/early").build(), null
        );
        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public MultipleResourcesVisitors multipleResourcesVisitors() {
                return new MultipleResourcesVisitors() {
                    @Override
                    public <REQUEST> MultipleResourcesVisitors.DataPreRetrievalPhase<?> onDataPreRetrieval(
                            REQUEST request, OperationMeta operationMeta,
                            MultipleResourcesJsonApiContext<REQUEST, ?, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return MultipleResourcesVisitors.DataPreRetrievalPhase.returnDoc(earlyDoc);
                    }
                };
            }
        };

        // when
        MultipleResourcesDoc<?> result = buildProcessor(plugin)
                .toMultipleResourcesDoc();

        // then
        assertThat(result).isSameAs(earlyDoc);
        verify(ds, never()).get(any());
    }

    // --- MUTATE_REQUEST at pre-retrieval ---

    @Test
    void plugin_mutateRequestAtPreRetrieval_dataSupplierReceivesMutatedRequest() {
        // given
        Request mutatedRequest = new Request(List.of("foo"));
        when(ds.get(mutatedRequest)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public MultipleResourcesVisitors multipleResourcesVisitors() {
                return new MultipleResourcesVisitors() {
                    @Override
                    public <REQ> MultipleResourcesVisitors.DataPreRetrievalPhase<?> onDataPreRetrieval(
                            REQ request, OperationMeta operationMeta,
                            MultipleResourcesJsonApiContext<REQ, ?, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return MultipleResourcesVisitors.DataPreRetrievalPhase.mutatedRequest(mutatedRequest);
                    }
                };
            }
        };

        // when
        MultipleResourcesDoc<?> result = buildProcessor(plugin)
                .toMultipleResourcesDoc();

        // then
        assertThat(result.getData()).hasSize(1);
        verify(ds, never()).get(REQUEST);
        verify(ds, times(1)).get(mutatedRequest);
    }

    // --- RETURN_DOC at relationships pre-retrieval ---

    @Test
    void plugin_returnDocAtRelationshipsPreRetrieval_shortCircuitsBeforeRelationshipResolution() {
        // given
        when(ds.get(REQUEST)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        MultipleResourcesDoc<?> overrideDoc = new MultipleResourcesDoc<>(
                List.of(new ResourceObject<>("override", null, "silver", null, null, null, null)),
                LinksObject.builder().self("/override").build(), null
        );

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public MultipleResourcesVisitors multipleResourcesVisitors() {
                return new MultipleResourcesVisitors() {
                    @Override
                    public <REQ, DST, DOC extends MultipleResourcesDoc<?>>
                    MultipleResourcesVisitors.RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
                            REQ request, OperationMeta operationMeta,
                            PaginationAwareResponse<DST> paginationAwareResponse, DOC doc,
                            MultipleResourcesJsonApiContext<REQ, DST, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return MultipleResourcesVisitors.RelationshipsPreRetrievalPhase.returnDoc(overrideDoc);
                    }
                };
            }
        };

        // when
        MultipleResourcesDoc<?> result = buildProcessor(plugin)
                .toMultipleResourcesDoc();

        // then
        assertThat(result).isSameAs(overrideDoc);
        verify(ds, times(1)).get(REQUEST); // data IS fetched
    }

    // --- MUTATE_DOC at relationships post-retrieval ---

    @Test
    void plugin_mutateDocAtRelationshipsPostRetrieval_returnsModifiedDoc() {
        // given
        when(ds.get(REQUEST)).thenReturn(PaginationAwareResponse.fromItemsNotPageable(List.of(DTO_1)));
        MultipleResourcesDoc<?> mutatedDoc = new MultipleResourcesDoc<>(
                Collections.emptyList(), null, "mutated-meta"
        );

        JsonApi4jPlugin plugin = new TestPlugin() {
            @Override
            public MultipleResourcesVisitors multipleResourcesVisitors() {
                return new MultipleResourcesVisitors() {
                    @Override
                    public <REQ, DST, DOC extends MultipleResourcesDoc<?>>
                    MultipleResourcesVisitors.RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
                            REQ request, OperationMeta operationMeta,
                            PaginationAwareResponse<DST> paginationAwareResponse, DOC doc,
                            MultipleResourcesJsonApiContext<REQ, DST, ?> context,
                            JsonApiPluginInfo pluginInfo) {
                        return MultipleResourcesVisitors.RelationshipsPostRetrievalPhase.mutatedDoc(mutatedDoc);
                    }
                };
            }
        };

        // when
        MultipleResourcesDoc<?> result = buildProcessor(plugin)
                .toMultipleResourcesDoc();

        // then
        assertThat(result).isSameAs(mutatedDoc);
        assertThat(result.getMeta()).isEqualTo("mutated-meta");
    }

    // --- Helpers ---

    private pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesTerminalStage<Request, Dto, ?> buildProcessor(JsonApi4jPlugin plugin) {
        PluginSettings settings = PluginSettings.builder()
                .plugin(plugin)
                .operationMeta(null)
                .info(new JsonApiPluginInfo(null, null, null))
                .build();

        return new MultipleResourcesProcessor()
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
