package pro.api4.jsonapi4j.compound.docs.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.IncludedGap;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonApiResponseWriterTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonApiResponseWriter sut = new JsonApiResponseWriter(MAPPER);

    @Nested
    class Compose {

        @Test
        void compose_gapsInAnyOrder_listsThemSortedByTypeThenReason() throws Exception {
            JsonNode document = MAPPER.readTree(sut.compose(MAPPER.createObjectNode(), List.of(), List.of(
                    new IncludedGap(IncompleteReason.NO_ROUTE, "regions"),
                    new IncludedGap(IncompleteReason.NO_ROUTE, "currencies"),
                    new IncludedGap(IncompleteReason.FETCH_FAILED, "currencies")
            )));

            assertThat(document.path("meta").path(JsonApiResponseWriter.INCLUDED_INCOMPLETE_META_FIELD).toString())
                    .isEqualTo("[{\"reason\":\"FETCH_FAILED\",\"type\":\"currencies\"},"
                            + "{\"reason\":\"NO_ROUTE\",\"type\":\"currencies\"},"
                            + "{\"reason\":\"NO_ROUTE\",\"type\":\"regions\"}]");
        }

        @Test
        void compose_metaNotAnObject_leavesItUntouched() throws Exception {
            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.put("meta", "invalid");

            JsonNode document = MAPPER.readTree(sut.compose(
                    rootNode,
                    List.of(),
                    List.of(new IncludedGap(IncompleteReason.NO_ROUTE, "regions"))
            ));

            assertThat(document.get("meta").asText()).isEqualTo("invalid");
        }

        @Test
        void compose_noGaps_addsNoMeta() throws Exception {
            JsonNode document = MAPPER.readTree(sut.compose(
                    MAPPER.createObjectNode(),
                    List.of("{\"type\":\"countries\",\"id\":\"NO\"}"),
                    List.of()
            ));

            assertThat(document.has("meta")).isFalse();
            assertThat(document.get("included")).hasSize(1);
        }

    }

}
