package pro.api4.jsonapi4j.plugin.oas.customizer;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.HttpStatusCodes;

import java.util.List;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Examples must be embedded as JSON, not as the source text that spells it. Swagger UI hides the difference — it
 * renders a JSON-looking string as a highlighted block — but the document then says the example of an
 * {@code ErrorsDoc} is a string, which contradicts the media type's schema and makes tools that build mock responses
 * return the escaped text instead of an error document.
 */
class ErrorExamplesCustomizerTests {

    private final ErrorExamplesCustomizer sut = new ErrorExamplesCustomizer();

    private Map<String, Example> registeredExamples() {
        OpenAPI openApi = new OpenAPI();
        sut.customise(openApi);
        return openApi.getComponents().getExamples();
    }

    @Test
    void customise_everyErrorExample_isEmbeddedAsJson() {
        assertThat(registeredExamples().values())
                .isNotEmpty()
                .allSatisfy(example -> assertThat(example.getValue()).isInstanceOf(JsonNode.class));
    }

    @Test
    void customise_everyErrorExample_carriesTheErrorsMember() {
        assertThat(registeredExamples().values())
                .allSatisfy(example -> assertThat(((JsonNode) example.getValue()).get("errors").isArray()).isTrue());
    }

    @Test
    void customise_registersAnExampleForEveryDocumentedErrorCode() {
        assertThat(registeredExamples().keySet())
                .containsExactlyInAnyOrderElementsOf(ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.values());
    }


    /**
     * The map drives the order error responses and their examples are written in. It must iterate deterministically:
     * a {@code Map.of(...)} here would randomize the order per JVM run and shuffle the published document between
     * restarts.
     */
    @Test
    public void codesToExampleName_always_iteratesInAscendingStatusCodeOrder() {
        List<Integer> codes = ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.keySet()
                .stream()
                .map(HttpStatusCodes::getCode)
                .toList();

        assertThat(codes).isSorted();
    }

}
