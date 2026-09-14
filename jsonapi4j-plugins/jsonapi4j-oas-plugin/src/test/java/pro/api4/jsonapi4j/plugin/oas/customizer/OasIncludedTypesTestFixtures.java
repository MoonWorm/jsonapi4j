package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;

/**
 * Two resources joined by a relationship that declares what it points at, which is what makes a document carry an
 * {@code included} member with more than one shape in it.
 */
final class OasIncludedTypesTestFixtures {

    static final String ARTICLES = "articles";
    static final String AUTHORS = "authors";
    static final String AUTHOR = "author";

    private OasIncludedTypesTestFixtures() {
    }

    static JsonApi4j jsonApi4j() {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(new DefaultOasProperties()))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new ArticleResource())
                        .resource(new AuthorResource())
                        .relationship(new AuthorRelationship())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(new ArticleOperations())
                        .build())
                .build();
    }

    @JsonApiResource(resourceType = ARTICLES)
    public static class ArticleResource implements Resource<ArticleAttributes> {

        @Override
        public String resolveResourceId(ArticleAttributes attributes) {
            return attributes.id();
        }

        @Override
        public ArticleAttributes resolveAttributes(ArticleAttributes attributes) {
            return attributes;
        }

    }

    @JsonApiResource(resourceType = AUTHORS)
    public static class AuthorResource implements Resource<AuthorAttributes> {

        @Override
        public String resolveResourceId(AuthorAttributes attributes) {
            return attributes.id();
        }

        @Override
        public AuthorAttributes resolveAttributes(AuthorAttributes attributes) {
            return attributes;
        }

    }

    public record ArticleAttributes(String id) {
    }

    public record AuthorAttributes(String id) {
    }

    @JsonApiResourceOperation(resource = ArticleResource.class)
    public static class ArticleOperations implements ResourceOperations<ArticleAttributes> {

        @Override
        public ArticleAttributes readById(JsonApiRequest request) {
            return new ArticleAttributes(request.getResourceId());
        }

    }

    @JsonApiRelationship(relationshipName = AUTHOR, parentResource = ArticleResource.class)
    @OasRelationshipInfo(relationshipTypes = AuthorResource.class)
    public static class AuthorRelationship implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String id) {
            return AUTHORS;
        }

        @Override
        public String resolveResourceIdentifierId(String id) {
            return id;
        }

    }

}
