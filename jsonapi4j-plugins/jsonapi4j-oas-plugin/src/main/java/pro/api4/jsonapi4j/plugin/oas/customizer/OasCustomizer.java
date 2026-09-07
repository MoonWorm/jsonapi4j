package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * One contribution to the generated OpenAPI document - tags and info, schemas, operations, error examples.
 * <p>
 * Customizers are applied in order to a single {@link OpenAPI} instance, each enriching what the previous ones
 * produced, so a later one may rely on the paths and schemas an earlier one registered. Host integrations that
 * drive their own document generation (e.g. springdoc) adapt these directly: the method signature is the one such
 * frameworks expect.
 *
 * @see OasDocument
 */
public interface OasCustomizer {

    /**
     * Enriches the document in place.
     *
     * @param openApi the document being generated, never {@code null}
     */
    void customise(OpenAPI openApi);

}
