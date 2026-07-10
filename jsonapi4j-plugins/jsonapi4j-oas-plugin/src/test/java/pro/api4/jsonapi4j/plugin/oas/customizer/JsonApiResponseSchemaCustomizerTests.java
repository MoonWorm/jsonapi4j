package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizerTestFixtures.build;

/**
 * Guards that {@link JsonApiResponseSchemaCustomizer} never documents the reserved meta types
 * ({@code state}/{@code plugins}/{@code config}/…) that get registered when {@code jsonapi4j.meta.enabled=true}.
 * <p>
 * The customizer derives the meta partition from {@link DomainRegistry#getMetaResourceTypes()} — there is no external
 * signal to pass or to drift out of sync. So a meta-enabled registry must produce exactly the same schemas (and no
 * extra paths) as an empty, meta-disabled one.
 */
class JsonApiResponseSchemaCustomizerTests {

    @Test
    void responseSchemas_excludeReservedMetaTypes() {
        Set<String> baseline = responseSchemaNames(build(false));
        JsonApi4j withMeta = build(true);

        // sanity: the meta-enabled registry really does carry reserved resources for the customizer to exclude —
        // so the equality below is a genuine exclusion, not a vacuous "there was nothing to document" pass.
        assertThat(withMeta.getDomainRegistry().isMetaEnabled()).isTrue();
        assertThat(withMeta.getDomainRegistry().getResources()).isNotEmpty();

        // yet none of the reserved resources leak into the schemas — identical to the empty baseline
        assertThat(responseSchemaNames(withMeta)).isEqualTo(baseline);
    }

    private static Set<String> responseSchemaNames(JsonApi4j jsonApi4j) {
        OpenAPI openApi = new OpenAPI();
        new JsonApiResponseSchemaCustomizer(jsonApi4j.getDomainRegistry(), jsonApi4j.getOperationsRegistry())
                .customise(openApi);
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return Set.of();
        }
        return openApi.getComponents().getSchemas().keySet();
    }
}
