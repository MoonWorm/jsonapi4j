package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2GrantFlow;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2Scope;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.plugin.oas.operation.model.PaginationStyle;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import java.util.List;

/**
 * Builds a single-resource {@link JsonApi4j} around one {@link OasOperationInfo}-annotated read operation, together
 * with the {@link OasProperties} naming the security schemes those annotations refer to. Shared by the customizer
 * tests that assert what an annotated operation contributes to the generated document.
 */
final class OasOperationTestFixtures {

    static final String SECURED_RESOURCE_TYPE = "secured";
    static final String MANDATORY = "mandatory";
    static final String SCOPE = "secured.read";
    static final String CUSTOM_SUMMARY = "Fetch one secured thing";
    static final String CUSTOM_DESCRIPTION = "Returns the secured thing the caller asked for.";
    static final String CUSTOM_ID_DESCRIPTION = "The secured thing's identifier";
    static final String CUSTOM_ID_EXAMPLE = "sec-42";
    static final String RESOURCE_ID_DESCRIPTION = "The secured resource id";
    static final String RESOURCE_ID_EXAMPLE = "sec-1";
    static final String RETIRED_RESOURCE_TYPE = "retired";

    private OasOperationTestFixtures() {
    }

    static JsonApi4j jsonApi4j(OasProperties oasProperties) {
        return jsonApi4j(oasProperties, new SecuredOperations());
    }

    static JsonApi4j jsonApi4j(OasProperties oasProperties,
                               ResourceOperations<SecuredAttributes> operations) {
        PluginRegistry plugins = PluginRegistry.builder().register(new JsonApiOasPlugin(oasProperties)).build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new SecuredResource())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(operations)
                        .build())
                .build();
    }

    /**
     * The same domain and operations, plus a stand-in for another plugin the OAS plugin reacts to. Registered by
     * name and nothing else, which is exactly how the OAS plugin matches its peers - depending on the real module
     * here would invert that.
     */
    static JsonApi4j jsonApi4jWithPeerPlugin(String peerPluginName,
                                             ResourceOperations<SecuredAttributes> operations) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(new DefaultOasProperties()))
                .register(new PeerPlugin(peerPluginName))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new SecuredResource())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(operations)
                        .build())
                .build();
    }

    record PeerPlugin(String pluginName) implements JsonApi4jPlugin {
    }

    /**
     * A resource declared as going away, with operations that say nothing about deprecation themselves.
     */
    static JsonApi4j jsonApi4jWithRetiredResource() {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(new DefaultOasProperties()))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new RetiredResource())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(new RetiredOperations())
                        .build())
                .build();
    }

    /**
     * The same domain and operations, but with no OAS plugin registered - the customizers then resolve no
     * {@link OasProperties} at all, which is how an app that never configured the plugin looks.
     */
    static JsonApi4j jsonApi4jWithoutOasPlugin() {
        PluginRegistry plugins = PluginRegistry.empty();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new SecuredResource())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(new SecuredOperations())
                        .build())
                .build();
    }

    static DefaultOasProperties oasProperties(String clientCredentialsName,
                                              String pkceName) {
        DefaultOAuth2 oauth2 = new DefaultOAuth2();
        if (clientCredentialsName != null) {
            oauth2.setClientCredentials(grantFlow(clientCredentialsName));
        }
        if (pkceName != null) {
            oauth2.setAuthorizationCodeWithPkce(grantFlow(pkceName));
        }
        DefaultOasProperties oasProperties = new DefaultOasProperties();
        oasProperties.setOauth2(oauth2);
        return oasProperties;
    }

    private static DefaultOAuth2GrantFlow grantFlow(String name) {
        DefaultOAuth2GrantFlow grantFlow = new DefaultOAuth2GrantFlow();
        grantFlow.setName(name);
        grantFlow.setTokenUrl("http://foo.bar/tokenUrl");
        grantFlow.setAuthorizationUrl("http://foo.bar/authorizationUrl");
        grantFlow.setScopes(List.of(scope()));
        return grantFlow;
    }

    private static DefaultOAuth2Scope scope() {
        DefaultOAuth2Scope scope = new DefaultOAuth2Scope();
        scope.setName(SCOPE);
        scope.setDescription("Read-only access to secured data");
        return scope;
    }

    @JsonApiResource(resourceType = SECURED_RESOURCE_TYPE)
    @OasResourceInfo(
            resourceIdDescription = RESOURCE_ID_DESCRIPTION,
            resourceIdExample = RESOURCE_ID_EXAMPLE
    )
    public static class SecuredResource implements Resource<SecuredAttributes> {

        @Override
        public String resolveResourceId(SecuredAttributes attributes) {
            return attributes.id();
        }

        @Override
        public SecuredAttributes resolveAttributes(SecuredAttributes attributes) {
            return attributes;
        }

    }

    public record SecuredAttributes(String id) {
    }

    /**
     * Attributes the resource declares mandatory. A response may still omit any of them - unset values are not
     * serialized, sparse fieldsets narrow the set, access control withholds what a caller may not see - so the two
     * directions disagree about what is required, which is what the split schema exists for.
     */
    public record MandatoryAttributes(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
            String nickname) {
    }

    @JsonApiResource(resourceType = MANDATORY)
    @OasResourceInfo(attributes = MandatoryAttributes.class, resourceNameSingle = "mandatory")
    public static class MandatoryResource implements Resource<MandatoryAttributes> {

        @Override
        public String resolveResourceId(MandatoryAttributes attributes) {
            return attributes.name();
        }

        @Override
        public MandatoryAttributes resolveAttributes(MandatoryAttributes attributes) {
            return attributes;
        }

    }

    /** Readable only - so no request-side attributes schema should be published. */
    @JsonApiResourceOperation(resource = MandatoryResource.class)
    public static class MandatoryReadOperations implements ResourceOperations<MandatoryAttributes> {

        @Override
        public MandatoryAttributes readById(JsonApiRequest request) {
            return new MandatoryAttributes("n", "e", null);
        }

    }

    /** Updatable only - so only the update form of the attributes schema is published. */
    @JsonApiResourceOperation(resource = MandatoryResource.class)
    public static class MandatoryUpdateOperations implements ResourceOperations<MandatoryAttributes> {

        @Override
        public MandatoryAttributes readById(JsonApiRequest request) {
            return new MandatoryAttributes("n", "e", null);
        }

        @Override
        public void update(JsonApiRequest request) {
        }

    }

    /** Readable and creatable - the create form of the attributes schema is published. */
    @JsonApiResourceOperation(resource = MandatoryResource.class)
    public static class MandatoryWriteOperations implements ResourceOperations<MandatoryAttributes> {

        @Override
        public MandatoryAttributes readById(JsonApiRequest request) {
            return new MandatoryAttributes("n", "e", null);
        }

        @Override
        public MandatoryAttributes create(JsonApiRequest request) {
            return new MandatoryAttributes("n", "e", null);
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true,
                    requiredScopes = SCOPE
            )
    )
    public static class SecuredOperations implements ResourceOperations<SecuredAttributes> {

        @Override
        public SecuredAttributes readById(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class DescribedOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(summary = CUSTOM_SUMMARY, description = CUSTOM_DESCRIPTION)
        @Override
        public SecuredAttributes readById(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class WriteOperations implements ResourceOperations<SecuredAttributes> {

        @Override
        public SecuredAttributes create(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

        @Override
        public void update(JsonApiRequest request) {
        }

        @Override
        public void delete(JsonApiRequest request) {
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class ListingOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(
                sortableFields = {"id", "createdAt"},
                pagination = {PaginationStyle.CURSOR, PaginationStyle.LIMIT_OFFSET}
        )
        @Override
        public PaginationAwareResponse<SecuredAttributes> readPage(JsonApiRequest request) {
            return PaginationAwareResponse.fromItemsNotPageable(List.of());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class FilterableListingOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(
                filters = {
                        @OasOperationInfo.Filter(name = "id", description = "Filter by id", example = "42"),
                        @OasOperationInfo.Filter(name = "region")
                }
        )
        @Override
        public PaginationAwareResponse<SecuredAttributes> readPage(JsonApiRequest request) {
            return PaginationAwareResponse.fromItemsNotPageable(List.of());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class OverriddenParamOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(
                parameters = {
                        @OasOperationInfo.Parameter(
                                name = "id",
                                in = In.PATH,
                                description = CUSTOM_ID_DESCRIPTION,
                                example = CUSTOM_ID_EXAMPLE
                        ),
                        @OasOperationInfo.Parameter(
                                name = "tenant",
                                description = "Tenant the request is scoped to",
                                example = "acme",
                                required = false
                        )
                }
        )
        @Override
        public SecuredAttributes readById(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class PartlyDeprecatedOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(deprecated = true)
        @Override
        public SecuredAttributes readById(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

        @Override
        public SecuredAttributes create(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    @JsonApiResource(resourceType = RETIRED_RESOURCE_TYPE)
    @OasResourceInfo(deprecated = true)
    public static class RetiredResource implements Resource<SecuredAttributes> {

        @Override
        public String resolveResourceId(SecuredAttributes attributes) {
            return attributes.id();
        }

        @Override
        public SecuredAttributes resolveAttributes(SecuredAttributes attributes) {
            return attributes;
        }

    }

    @JsonApiResourceOperation(resource = RetiredResource.class)
    public static class RetiredOperations implements ResourceOperations<SecuredAttributes> {

        @Override
        public SecuredAttributes readById(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

        @Override
        public SecuredAttributes create(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class CursorOnlyListingOperations implements ResourceOperations<SecuredAttributes> {

        @Override
        public PaginationAwareResponse<SecuredAttributes> readPage(JsonApiRequest request) {
            return PaginationAwareResponse.fromItemsNotPageable(List.of());
        }

    }

    @JsonApiResourceOperation(resource = SecuredResource.class)
    public static class CustomPayloadOperations implements ResourceOperations<SecuredAttributes> {

        @OasOperationInfo(payloadType = CustomPayload.class)
        @Override
        public SecuredAttributes create(JsonApiRequest request) {
            return new SecuredAttributes(request.getResourceId());
        }

    }

    public record CustomPayload(String reference) {
    }

}
