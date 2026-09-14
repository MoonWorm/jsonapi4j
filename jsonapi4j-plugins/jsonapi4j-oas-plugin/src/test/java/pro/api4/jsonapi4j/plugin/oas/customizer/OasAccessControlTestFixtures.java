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
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
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

    static final String READ = "guarded.read";
    static final String WRITE = "guarded.write";
    static final String ADMIN = "admin.full";
    static final String SCOPES_REASON = "read access to guarded data, or full admin access";
    static final String ENTITLEMENTS_REASON = "internal support staff";

    private OasAccessControlTestFixtures() {
    }

    static JsonApi4j jsonApi4j(String... declaredScopes) {
        return jsonApi4j(new GuardedOperations(), declaredScopes);
    }

    static JsonApi4j jsonApi4j(ResourceOperations<GuardedAttributes> operations,
                               String... declaredScopes) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(oasProperties(declaredScopes)))
                .register(new JsonApiAccessControlPlugin(new DefaultAccessControlEvaluator(), new DefaultAcProperties()))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins).resource(new GuardedResource()).build())
                .operationsRegistry(OperationsRegistry.builder(plugins).operations(operations).build())
                .build();
    }

    private static DefaultOasProperties oasProperties(String... declaredScopes) {
        DefaultOAuth2GrantFlow grantFlow = new DefaultOAuth2GrantFlow();
        grantFlow.setName(SCHEME);
        grantFlow.setTokenUrl("http://foo.bar/tokenUrl");
        grantFlow.setScopes(Arrays.stream(declaredScopes).map(name -> {
            DefaultOAuth2Scope scope = new DefaultOAuth2Scope();
            scope.setName(name);
            scope.setDescription(name);
            return scope;
        }).toList());

        DefaultOAuth2 oauth2 = new DefaultOAuth2();
        oauth2.setClientCredentials(grantFlow);

        DefaultOasProperties oasProperties = new DefaultOasProperties();
        oasProperties.setOauth2(oauth2);
        return oasProperties;
    }

    @JsonApiResource(resourceType = GUARDED)
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

    public record GuardedAttributes(String id) {
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
            return new GuardedAttributes(request.getResourceId());
        }

        @AccessControl(
                scopes = @AccessControlScopes(@ScopesGroup(WRITE)),
                entitlements = @AccessControlEntitlements(
                        description = ENTITLEMENTS_REASON,
                        value = @EntitlementsGroup("SUPPORT")))
        @Override
        public GuardedAttributes create(JsonApiRequest request) {
            return new GuardedAttributes(request.getResourceId());
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
            return new GuardedAttributes(request.getResourceId());
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
            return new GuardedAttributes(request.getResourceId());
        }

    }

    static List<String> allScopes() {
        return List.of(READ, WRITE, ADMIN);
    }

}
