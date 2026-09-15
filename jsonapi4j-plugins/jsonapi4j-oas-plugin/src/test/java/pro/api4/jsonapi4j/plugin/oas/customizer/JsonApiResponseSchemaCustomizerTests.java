package pro.api4.jsonapi4j.plugin.oas.customizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.MandatoryAttributes;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.MandatoryReadOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.MandatoryResource;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.MandatoryWriteOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredResource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizerTestFixtures.build;

class JsonApiResponseSchemaCustomizerTests {

    private static JsonApi4j withOperations(ResourceOperations<MandatoryAttributes> operations) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(new DefaultOasProperties()))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins).resource(new MandatoryResource()).build())
                .operationsRegistry(OperationsRegistry.builder(plugins).operations(operations).build())
                .build();
    }

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
    /**
     * A response cannot promise an attribute is present: one with no value is not serialized, sparse fieldsets let a
     * client ask for a subset, and access control withholds what the caller may not see. What a *write* must carry is
     * a different question, so it gets a schema of its own rather than the two directions disagreeing under one name.
     */
    @Nested
    class RequiredAttributes {

        @Test
        void customise_attributesTheResourceDeclaresRequired_areNotRequiredInTheResponseSchema() {
            Map<String, Schema> schemas = responseSchemas(withOperations(new MandatoryWriteOperations()));

            assertThat(schemas.get("MandatoryAttributes").getRequired()).isNullOrEmpty();
        }

        @Test
        void customise_relaxedAttributes_sayWhyNothingIsGuaranteed() {
            Map<String, Schema> schemas = responseSchemas(withOperations(new MandatoryWriteOperations()));

            assertThat(schemas.get("MandatoryAttributes").getDescription())
                    .contains("No attribute is guaranteed to be present")
                    .contains("MandatoryCreateAttributes");
        }

        @Test
        void customise_readOnlyResource_doesNotPointAtARequestSchemaThatIsNeverPublished() {
            Map<String, Schema> schemas = responseSchemas(withOperations(new MandatoryReadOperations()));

            assertThat(schemas.get("MandatoryAttributes").getDescription())
                    .contains("No attribute is guaranteed to be present")
                    .doesNotContain("MandatoryCreateAttributes");
            assertThat(schemas).doesNotContainKey("MandatoryCreateAttributes");
        }

        /**
         * Nothing to relax means nothing to explain: a resource whose attributes were never declared required gets
         * the schema it always had.
         */
        @Test
        void customise_attributesWithNothingRequired_areLeftUndescribed() {
            PluginRegistry plugins = PluginRegistry.builder()
                    .register(new JsonApiOasPlugin(new DefaultOasProperties()))
                    .build();
            Map<String, Schema> schemas = responseSchemas(JsonApi4j.builder()
                    .pluginRegistry(plugins)
                    .domainRegistry(DomainRegistry.builder(plugins).resource(new SecuredResource()).build())
                    .operationsRegistry(OperationsRegistry.builder(plugins).operations(new SecuredOperations()).build())
                    .build());

            assertThat(schemas.get("SecuredAttributes").getRequired()).isNullOrEmpty();
            assertThat(schemas.get("SecuredAttributes").getDescription()).isNull();
        }

    }

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
     * JSON:API keys everything on {@code type}: a resource has exactly one, a mismatched one in a request body is a
     * {@code 409}, and it is what tells apart the members of {@code included}. A document that types it as an open
     * string states none of that.
     */
    @Nested
    class ResourceTypeMember {

        @Test
        void customise_resourceSchema_pinsItsTypeToTheOneAllowedValue() {
            Map<String, Schema> schemas = responseSchemas(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThat(((Schema) schemas.get("ArticlesResource").getProperties().get("type")).getEnum())
                    .containsExactly(OasIncludedTypesTestFixtures.ARTICLES);
            assertThat(((Schema) schemas.get("AuthorsResource").getProperties().get("type")).getEnum())
                    .containsExactly(OasIncludedTypesTestFixtures.AUTHORS);
        }

        @Test
        void customise_resourceSchema_requiresTheTypeItPins() {
            Map<String, Schema> schemas = responseSchemas(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThat(schemas.get("ArticlesResource").getRequired()).contains("type");
        }

    }

    /**
     * The discriminator is only usable if every schema it maps to really exists and really requires the property it
     * keys on - OpenAPI says so, and a generator that trusts a dangling mapping produces a client that cannot
     * deserialize the union at all.
     */
    @Nested
    class IncludedDiscriminator {

        @Test
        void customise_includedUnion_discriminatesOnTheTypeMember() {
            assertThat(includedItems().getDiscriminator().getPropertyName()).isEqualTo("type");
        }

        @Test
        void customise_includedUnion_mapsEveryTypeItCanCarry() {
            assertThat(includedItems().getDiscriminator().getMapping())
                    .containsEntry(OasIncludedTypesTestFixtures.AUTHORS, "#/components/schemas/AuthorsResource");
        }

        @Test
        void customise_includedUnion_mapsOnlyToSchemasTheDocumentDeclares() {
            Map<String, Schema> schemas = responseSchemas(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThat(includedItems().getDiscriminator().getMapping().values())
                    .allSatisfy(ref -> assertThat(schemas)
                            .containsKey(ref.substring("#/components/schemas/".length())));
        }

        @Test
        void customise_everyMappedSchema_requiresTheDiscriminatorProperty() {
            Map<String, Schema> schemas = responseSchemas(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThat(includedItems().getDiscriminator().getMapping().values())
                    .allSatisfy(ref -> assertThat(schemas.get(ref.substring("#/components/schemas/".length())).getRequired())
                            .contains("type"));
        }

        private Schema includedItems() {
            Schema included = (Schema) responseSchemas(OasIncludedTypesTestFixtures.jsonApi4j())
                    .get("ArticlesSingleResourceDoc")
                    .getProperties()
                    .get("included");
            return included.getItems();
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
