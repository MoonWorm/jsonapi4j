package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.Integration;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import java.util.List;
import java.util.Map;

/**
 * Builds a {@link JsonApi4j} whose only registered resources/operations are the built-in meta API (when
 * {@code metaEnabled}), or an empty, meta-disabled instance otherwise. Shared by the OAS customizer tests that
 * assert reserved meta types never leak into the generated document.
 */
final class OasCustomizerTestFixtures {

    private OasCustomizerTestFixtures() {
    }

    static JsonApi4j build(boolean metaEnabled) {
        var builder = JsonApi4j.builder()
                .domainRegistry(DomainRegistry.builder(List.of()).build())
                .operationsRegistry(OperationsRegistry.builder(List.of()).build());
        if (metaEnabled) {
            builder.meta(MetaContext.of(
                    Map.of(JsonApi4jProperties.ROOT_PATH_PROPERTY, JsonApi4jProperties.DEFAULT_ROOT_PATH),
                    Integration.SERVLET));
        }
        return builder.build();
    }
}
