package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizerTestFixtures.build;

/**
 * Guards that {@link JsonApiOperationsCustomizer} never documents operation paths for the reserved meta types that get
 * registered when {@code jsonapi4j.meta.enabled=true}.
 * <p>
 * The customizer derives the meta partition from {@link DomainRegistry#getMetaResourceTypes()}, so a registry whose
 * only configured operations are the meta ones yields no paths at all.
 */
class JsonApiOperationsCustomizerTests {

    @Test
    void operationPaths_excludeReservedMetaTypes() {
        JsonApi4j withMeta = build(true);
        assertThat(withMeta.getDomainRegistry().isMetaEnabled()).isTrue();

        OpenAPI openApi = new OpenAPI();
        new JsonApiOperationsCustomizer(
                "/jsonapi",
                withMeta.getDomainRegistry(),
                withMeta.getOperationsRegistry(),
                List.of()
        ).customise(openApi);

        // the only configured operations are the meta ones, and every one of them is excluded — so no paths at all
        Set<String> paths = openApi.getPaths() == null ? Set.of() : openApi.getPaths().keySet();
        assertThat(paths).isEmpty();
    }
}
