package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the generated OAS never documents the reserved meta types ({@code state}/{@code plugins}/{@code config}/…)
 * that get registered into the live registries when {@code jsonapi4j.meta.enabled=true}.
 * <p>
 * The customizers now derive the meta partition from {@link DomainRegistry#getMetaResourceTypes()} — there is no
 * external signal to pass or to drift out of sync. So a meta-enabled registry must produce exactly the same schemas
 * (and no extra paths) as an empty, meta-disabled one.
 */
class OasMetaExclusionTests {

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

    private static JsonApi4j build(boolean metaEnabled) {
        var builder = JsonApi4j.builder()
                .domainRegistry(DomainRegistry.builder(List.of()).build())
                .operationsRegistry(OperationsRegistry.builder(List.of()).build());
        if (metaEnabled) {
            builder.meta(MetaContext.of(Map.of(JsonApi4jProperties.ROOT_PATH_PROPERTY, JsonApi4jProperties.DEFAULT_ROOT_PATH), MetaContext.Integration.SERVLET));
        }
        return builder.build();
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
