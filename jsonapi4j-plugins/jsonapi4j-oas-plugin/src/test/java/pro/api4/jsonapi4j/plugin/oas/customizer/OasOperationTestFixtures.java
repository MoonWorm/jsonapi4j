package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2GrantFlow;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2Scope;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;

/**
 * Builds a single-resource {@link JsonApi4j} around one {@link OasOperationInfo}-annotated read operation, together
 * with the {@link OasProperties} naming the security schemes those annotations refer to. Shared by the customizer
 * tests that assert what an annotated operation contributes to the generated document.
 */
final class OasOperationTestFixtures {

    static final String SECURED_RESOURCE_TYPE = "secured";
    static final String SCOPE = "secured.read";
    static final String CUSTOM_SUMMARY = "Fetch one secured thing";
    static final String CUSTOM_DESCRIPTION = "Returns the secured thing the caller asked for.";

    private OasOperationTestFixtures() {
    }

    static JsonApi4j jsonApi4j(OasProperties oasProperties) {
        return jsonApi4j(oasProperties, new SecuredOperations());
    }

    static JsonApi4j jsonApi4j(OasProperties oasProperties,
                               ResourceOperations<SecuredAttributes> operations) {
        List<JsonApi4jPlugin> plugins = List.of(new JsonApiOasPlugin(oasProperties));
        return JsonApi4j.builder()
                .plugins(plugins)
                .domainRegistry(DomainRegistry.builder(plugins)
                        .resource(new SecuredResource())
                        .build())
                .operationsRegistry(OperationsRegistry.builder(plugins)
                        .operations(operations)
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

}
