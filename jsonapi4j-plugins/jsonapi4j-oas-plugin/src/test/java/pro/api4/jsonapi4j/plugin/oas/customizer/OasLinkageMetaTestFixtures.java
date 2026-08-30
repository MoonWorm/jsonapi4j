package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.ToOneRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;

import java.util.List;

/**
 * A resource carrying one to-one and one to-many relationship, both declaring resource linkage meta. Exists because
 * the sample apps only exercise the to-many case, and the to-one branch is where the custom linkage schemas used to be
 * built, referenced, and never registered.
 */
final class OasLinkageMetaTestFixtures {

    static final String OWNED_RESOURCE_TYPE = "owned";
    static final String KEEPER = "keeper";
    static final String WATCHERS = "watchers";

    private OasLinkageMetaTestFixtures() {
    }

    static JsonApi4j jsonApi4j() {
        return build(OperationsRegistry.builder(plugins()).build());
    }

    static JsonApi4j jsonApi4jWithWrites() {
        return build(OperationsRegistry.builder(plugins())
                .operations(new KeeperOperations())
                .operations(new WatchersOperations())
                .build());
    }

    private static List<JsonApi4jPlugin> plugins() {
        return List.of(new JsonApiOasPlugin(new DefaultOasProperties()));
    }

    private static JsonApi4j build(OperationsRegistry operationsRegistry) {
        List<JsonApi4jPlugin> plugins = plugins();
        return JsonApi4j.builder()
                .plugins(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new OwnedResource())
                        .relationship(new KeeperRelationship())
                        .relationship(new WatchersRelationship())
                        .build())
                .operationsRegistry(operationsRegistry)
                .build();
    }

    @JsonApiResource(resourceType = OWNED_RESOURCE_TYPE)
    public static class OwnedResource implements Resource<OwnedAttributes> {

        @Override
        public String resolveResourceId(OwnedAttributes attributes) {
            return attributes.id();
        }

        @Override
        public OwnedAttributes resolveAttributes(OwnedAttributes attributes) {
            return attributes;
        }

    }

    public record OwnedAttributes(String id) {
    }

    public record LinkageMeta(String since) {
    }

    @JsonApiRelationship(relationshipName = KEEPER, parentResource = OwnedResource.class)
    @OasRelationshipInfo(resourceLinkageMetaType = LinkageMeta.class)
    public static class KeeperRelationship implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String id) {
            return OWNED_RESOURCE_TYPE;
        }

        @Override
        public String resolveResourceIdentifierId(String id) {
            return id;
        }

    }

    @JsonApiRelationship(relationshipName = WATCHERS, parentResource = OwnedResource.class)
    @OasRelationshipInfo(resourceLinkageMetaType = LinkageMeta.class)
    public static class WatchersRelationship implements ToManyRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String id) {
            return OWNED_RESOURCE_TYPE;
        }

        @Override
        public String resolveResourceIdentifierId(String id) {
            return id;
        }

    }

    @JsonApiRelationshipOperation(relationship = KeeperRelationship.class)
    public static class KeeperOperations implements ToOneRelationshipOperations<OwnedAttributes, String> {

        @Override
        public void update(JsonApiRequest request) {
        }

    }

    @JsonApiRelationshipOperation(relationship = WatchersRelationship.class)
    public static class WatchersOperations implements ToManyRelationshipOperations<OwnedAttributes, String> {

        @Override
        public void update(JsonApiRequest request) {
        }

    }

}
