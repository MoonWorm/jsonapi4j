package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.config.DefaultAcProperties;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2GrantFlow;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultOAuth2Scope;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.Arrays;
import java.util.List;

/**
 * A resource whose operations declare access control requirements, together with an OpenAPI configuration whose
 * grant flow declares the scopes they name.
 */
final class OasAccessControlTestFixtures {

    static final String GUARDED = "guarded";
    static final String SCHEME = "m2m";
    static final String PKCE_SCHEME = "pkce";

    static final String READ = "guarded.read";
    static final String WRITE = "guarded.write";
    static final String ADMIN = "admin.full";
    static final String SENSITIVE = "guarded.sensitive";
    static final String SENSITIVE_REASON = "a support role";
    static final String SCOPES_REASON = "read access to guarded data, or full admin access";
    static final String ENTITLEMENTS_REASON = "internal support staff";

    private OasAccessControlTestFixtures() {
    }

    static JsonApi4j jsonApi4j(String... declaredScopes) {
        return jsonApi4j(new GuardedOperations(), declaredScopes);
    }

    static JsonApi4j jsonApi4j(ResourceOperations<GuardedAttributes> operations,
                               String... declaredScopes) {
        return jsonApi4j(operations, oasProperties(declaredScopes));
    }

    /**
     * Two grant flows declaring different scopes - the shape that tells a scope matched against its own scheme apart
     * from one matched against the union of every flow's.
     */
    static JsonApi4j twoFlowJsonApi4j(ResourceOperations<GuardedAttributes> operations,
                                      List<String> clientCredentialsScopes,
                                      List<String> pkceScopes) {
        return twoFlowJsonApi4j(operations, clientCredentialsScopes, pkceScopes, DiagnosticsMode.WARN);
    }

    static JsonApi4j twoFlowJsonApi4j(ResourceOperations<GuardedAttributes> operations,
                                      List<String> clientCredentialsScopes,
                                      List<String> pkceScopes,
                                      DiagnosticsMode diagnostics) {
        DefaultOAuth2 oauth2 = new DefaultOAuth2();
        oauth2.setClientCredentials(grantFlow(SCHEME, clientCredentialsScopes));
        DefaultOAuth2GrantFlow pkce = grantFlow(PKCE_SCHEME, pkceScopes);
        pkce.setAuthorizationUrl("http://foo.bar/authorizationUrl");
        oauth2.setAuthorizationCodeWithPkce(pkce);

        DefaultOasProperties oasProperties = new DefaultOasProperties();
        oasProperties.setOauth2(oauth2);
        oasProperties.setDiagnostics(diagnostics);
        return jsonApi4j(operations, oasProperties);
    }

    private static JsonApi4j jsonApi4j(ResourceOperations<GuardedAttributes> operations,
                                       DefaultOasProperties oasProperties) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(oasProperties))
                .register(new JsonApiAccessControlPlugin(new DefaultAccessControlEvaluator(), new DefaultAcProperties()))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins).resource(new GuardedResource()).build())
                .operationsRegistry(OperationsRegistry.builder(plugins).operations(operations).build())
                .build();
    }

    private static DefaultOasProperties oasProperties(String... declaredScopes) {
        DefaultOAuth2 oauth2 = new DefaultOAuth2();
        oauth2.setClientCredentials(grantFlow(SCHEME, Arrays.asList(declaredScopes)));

        DefaultOasProperties oasProperties = new DefaultOasProperties();
        oasProperties.setOauth2(oauth2);
        return oasProperties;
    }

    private static DefaultOAuth2GrantFlow grantFlow(String name,
                                                    List<String> declaredScopes) {
        DefaultOAuth2GrantFlow grantFlow = new DefaultOAuth2GrantFlow();
        grantFlow.setName(name);
        grantFlow.setTokenUrl("http://foo.bar/tokenUrl");
        grantFlow.setScopes(declaredScopes.stream().map(scopeName -> {
            DefaultOAuth2Scope scope = new DefaultOAuth2Scope();
            scope.setName(scopeName);
            scope.setDescription(scopeName);
            return scope;
        }).toList());
        return grantFlow;
    }

    @JsonApiResource(resourceType = GUARDED)
    @OasResourceInfo(attributes = GuardedAttributes.class)
    public static class GuardedResource implements Resource<GuardedAttributes> {

        @Override
        public String resolveResourceId(GuardedAttributes attributes) {
            return attributes.id();
        }

        @Override
        public GuardedAttributes resolveAttributes(GuardedAttributes attributes) {
            return attributes;
        }

    }

    @AccessControl(authenticated = Authenticated.AUTHENTICATED)
    public record GuardedAttributes(
            String id,
            @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE))) String secret,
            @AccessControl(
                    scopes = @AccessControlScopes(description = SENSITIVE_REASON, value = @ScopesGroup(SENSITIVE)))
            String described) {
    }

    @JsonApiResourceOperation(resource = GuardedResource.class)
    public static class GuardedOperations implements ResourceOperations<GuardedAttributes> {

        @AccessControl(scopes = @AccessControlScopes(
                mode = AccessControlScopes.Mode.ANY_OF,
                description = SCOPES_REASON,
                value = {
                        @ScopesGroup({READ, WRITE}),
                        @ScopesGroup(ADMIN)
                }))
        @Override
        public GuardedAttributes readById(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

        @AccessControl(
                scopes = @AccessControlScopes(@ScopesGroup(WRITE)),
                entitlements = @AccessControlEntitlements(
                        description = ENTITLEMENTS_REASON,
                        value = @EntitlementsGroup("SUPPORT")))
        @Override
        public GuardedAttributes create(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

        @AccessControl(authenticated = Authenticated.ANONYMOUS)
        @Override
        public void delete(JsonApiRequest request) {
        }

        @Override
        public void update(JsonApiRequest request) {
        }

    }

    /**
     * Requires a scope the OpenAPI configuration is not told about.
     */
    @JsonApiResourceOperation(resource = GuardedResource.class)
    public static class UndeclaredScopeOperations implements ResourceOperations<GuardedAttributes> {

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("never.declared")))
        @Override
        public GuardedAttributes readById(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

    }

    /**
     * Requires two scopes at once, so that a configuration splitting them across two grant flows leaves neither flow
     * able to carry the requirement.
     */
    @JsonApiResourceOperation(resource = GuardedResource.class)
    public static class BothScopesOperations implements ResourceOperations<GuardedAttributes> {

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup({READ, WRITE})))
        @Override
        public GuardedAttributes readById(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

    }

    /**
     * Declares its scopes through {@code @OasOperationInfo} rather than access control, and opts into both grant
     * flows - the other path a required scope reaches the document by.
     */
    @JsonApiResourceOperation(resource = GuardedResource.class)
    public static class DeclaredScopeOperations implements ResourceOperations<GuardedAttributes> {

        @OasOperationInfo(securityConfig = @OasOperationInfo.SecurityConfig(
                clientCredentialsSupported = true,
                pkceSupported = true,
                requiredScopes = READ))
        @Override
        public GuardedAttributes readById(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

    }

    /**
     * {@code (READ or WRITE) and (ADMIN or READ)} - the shape that only exists once expanded into alternatives.
     */
    @JsonApiResourceOperation(resource = GuardedResource.class)
    public static class CrossProductOperations implements ResourceOperations<GuardedAttributes> {

        @AccessControl(scopes = @AccessControlScopes(
                mode = AccessControlScopes.Mode.ALL_OF,
                value = {
                        @ScopesGroup(value = {READ, WRITE}, mode = ScopesGroup.Mode.ANY_OF),
                        @ScopesGroup(value = {ADMIN, READ}, mode = ScopesGroup.Mode.ANY_OF)
                }))
        @Override
        public GuardedAttributes readById(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId(), null, null);
        }

    }

    static List<String> allScopes() {
        return List.of(READ, WRITE, ADMIN);
    }

}
