package pro.api4.jsonapi4j.plugin.oas.customizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.domain.DomainRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizerTestFixtures.build;

class JsonApiResponseSchemaCustomizerTests {

    private static Map<String, Schema> responseSchemas(JsonApi4j jsonApi4j) {
        OpenAPI openApi = new OpenAPI();
        JsonApiResponseSchemaCustomizer sut = new JsonApiResponseSchemaCustomizer(jsonApi4j);
        sut.customise(openApi);

        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return Map.of();
        }
        return openApi.getComponents().getSchemas();
    }

    /**
     * The customizer derives the meta partition from {@link DomainRegistry#getMetaResourceTypes()} — there is no
     * external signal to pass or to drift out of sync. So a meta-enabled registry must produce exactly the same
     * schemas as an empty, meta-disabled one.
     */
    @Nested
    class MetaTypes {

        @Test
        void customise_metaEnabledRegistry_documentsSameSchemasAsEmptyOne() {
            Set<String> baseline = responseSchemas(build(false)).keySet();
            JsonApi4j withMeta = build(true);

            assertThat(withMeta.getDomainRegistry().isMetaEnabled()).isTrue();
            assertThat(withMeta.getDomainRegistry().getResources()).isNotEmpty();

            assertThat(responseSchemas(withMeta).keySet()).isEqualTo(baseline);
        }

    }

    /**
     * The single-resource and multiple-resources documents describe what an operation returns, so they are published
     * for the operations that return them rather than for any resource that happens to have operations at all.
     */
    @Nested
    class DocumentsPerOperation {

        @Test
        void customise_readByIdNotConfigured_publishesNoSingleResourceDoc() {
            Map<String, Schema> schemas = responseSchemas(OasLinkageMetaTestFixtures.jsonApi4jWithWrites());

            assertThat(schemas).doesNotContainKeys("OwnedSingleResourceDoc", "OwnedMultipleResourcesDoc");
        }

    }

    /**
     * A relationship declaring resource linkage meta gets a document of its own, whose linkage carries a typed
     * {@code meta}. Both the document and everything it pulls in must end up registered — a schema that is referenced
     * and never registered is a dangling {@code $ref}.
     */
    @Nested
    class LinkageMeta {

        @Test
        void customise_toOneRelationshipWithLinkageMeta_registersEverythingItReferences() {
            Map<String, Schema> schemas = responseSchemas(OasLinkageMetaTestFixtures.jsonApi4j());

            assertThat(schemas).containsKeys(
                    "OwnedKeeperToOneRelationshipDoc",
                    "OwnedKeeperResourceIdentifier",
                    "OwnedKeeperResourceIdentifierMeta"
            );
        }

        @Test
        void customise_toManyRelationshipWithLinkageMeta_registersEverythingItReferences() {
            Map<String, Schema> schemas = responseSchemas(OasLinkageMetaTestFixtures.jsonApi4j());

            assertThat(schemas).containsKeys(
                    "OwnedWatchersToManyRelationshipsDoc",
                    "OwnedWatchersResourceIdentifier",
                    "OwnedWatchersResourceIdentifierMeta"
            );
        }

        @Test
        void customise_relationshipsWithLinkageMeta_leaveNoDanglingReferences() {
            Map<String, Schema> schemas = responseSchemas(OasLinkageMetaTestFixtures.jsonApi4j());

            assertThat(referencedSchemaNames(schemas)).isSubsetOf(schemas.keySet());
        }

        @Test
        void customise_linkageMetaDeclared_typesTheLinkageMetaMember() {
            Map<String, Schema> schemas = responseSchemas(OasLinkageMetaTestFixtures.jsonApi4j());

            assertThat(((Schema<?>) schemas.get("OwnedKeeperResourceIdentifier").getProperties().get("meta")).get$ref())
                    .isEqualTo("#/components/schemas/OwnedKeeperResourceIdentifierMeta");
            assertThat(schemas.get("OwnedKeeperResourceIdentifierMeta").getProperties()).containsKey("since");
        }

    }

    private static Set<String> referencedSchemaNames(Map<String, Schema> schemas) {
        String rendered;
        try {
            rendered = JsonApi4jConfigReader.getJsonObjectMapper().writeValueAsString(schemas);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to render the generated schemas", e);
        }
        Matcher matcher = Pattern.compile("#/components/schemas/(\\w+)").matcher(rendered);
        Set<String> referenced = new HashSet<>();
        while (matcher.find()) {
            referenced.add(matcher.group(1));
        }
        return referenced;
    }

}
