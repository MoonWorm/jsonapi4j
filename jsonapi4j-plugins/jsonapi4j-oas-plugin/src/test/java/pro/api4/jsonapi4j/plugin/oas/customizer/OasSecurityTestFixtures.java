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
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;

/**
 * Builds a single-resource {@link JsonApi4j} whose only operation declares both OAuth2 grant flows, together with the
 * {@link OasProperties} that name the corresponding security schemes. Shared by the tests that assert operation-level
 * security requirements name the schemes exactly as {@link CommonOpenApiCustomizer} declares them.
 */
final class OasSecurityTestFixtures {

    static final String SECURED_RESOURCE_TYPE = "secured";
    static final String SCOPE = "secured.read";

    private OasSecurityTestFixtures() {
    }

    static JsonApi4j jsonApi4j(OasProperties oasProperties) {
        List<JsonApi4jPlugin> plugins = List.of(new JsonApiOasPlugin(oasProperties));
        return JsonApi4j.builder()
                .plugins(plugins)
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
        return grantFlow;
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

}
