package pro.api4.jsonapi4j.rest.quarkus.runtime.principal;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.servlet.ServletRequest;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuarkusJwtPrincipalResolverTests {

    private static final String ENTITLEMENTS_CLAIM = "entitlements";

    private final ServletRequest request = mock(ServletRequest.class);

    private JsonWebToken jwt;

    private static Map<String, Object> claims(Object... keysAndValues) {
        Map<String, Object> claims = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            claims.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return claims;
    }

    @BeforeEach
    void setUp() {
        jwt = mock(JsonWebToken.class);
    }

    /**
     * Stubs the token the way SmallRye exposes it: {@link JsonWebToken#getClaimNames()} lists the claims and
     * {@link JsonWebToken#getClaim(String)} returns each value.
     */
    private void givenClaims(Map<String, Object> claims) {
        when(jwt.getClaimNames()).thenReturn(claims.keySet());
        when(jwt.getClaim(anyString())).thenAnswer(invocation -> claims.get(invocation.<String>getArgument(0)));
    }

    // --- plain Java claim values, as returned for registered claims ---

    @Test
    void resolvesUserIdAndScopesFromRegisteredClaims() {
        givenClaims(claims("sub", "user-42", "scope", "read write"));

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    // --- JSON-P claim values, as returned for custom claims ---

    @Test
    void unwrapsJsonStringClaimsRatherThanKeepingJsonQuoting() {
        givenClaims(claims(
                "sub", Json.createValue("user-42"),
                "email", Json.createValue("user@api4.pro")
        ));

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        assertThat(resolver.resolvePrincipal(request).attributes()).containsEntry("email", "user@api4.pro");
    }

    @Test
    void unwrapsJsonStringScopeClaim() {
        givenClaims(claims("scope", Json.createValue("read write")));

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void unwrapsJsonArrayScopeClaim() {
        givenClaims(claims("scp", Json.createArrayBuilder().add("read").add("write").build()));

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void unwrapsNestedJsonObjectClaimsForPathLookups() {
        givenClaims(claims(
                "sub", Json.createValue("user-42"),
                "realm_access", Json.createObjectBuilder()
                        .add("roles", Json.createArrayBuilder().add("admin"))
                        .build()
        ));
        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt,
                new ClaimsPrincipalMapper("sub", "realm_access.roles", null));

        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactly("admin");
    }

    @Test
    void unwrapsJsonNumbersAndBooleans() {
        givenClaims(claims(
                "exp", Json.createValue(1893456000),
                "ratio", Json.createValue(1.5),
                "email_verified", JsonValue.TRUE,
                "revoked", JsonValue.FALSE,
                "deleted_at", JsonValue.NULL
        ));

        Map<String, Object> attributes = new QuarkusJwtPrincipalResolver(jwt).resolvePrincipal(request).attributes();

        assertThat(attributes)
                .containsEntry("exp", 1893456000L)
                .containsEntry("ratio", 1.5)
                .containsEntry("email_verified", Boolean.TRUE)
                .containsEntry("revoked", Boolean.FALSE)
                .containsEntry("deleted_at", null);
    }

    @Test
    void unwrapsNestedStructuresInAttributes() {
        givenClaims(claims("resource_access", Json.createObjectBuilder()
                .add("jsonapi4j", Json.createObjectBuilder()
                        .add("roles", Json.createArrayBuilder().add("admin").add("audit")))
                .build()));

        Map<String, Object> attributes = new QuarkusJwtPrincipalResolver(jwt).resolvePrincipal(request).attributes();

        assertThat(attributes).containsEntry("resource_access",
                Map.of("jsonapi4j", Map.of("roles", List.of("admin", "audit"))));
    }

    // --- entitlements ---

    @Test
    void resolvesEntitlementsFromConfiguredClaim() {
        givenClaims(claims("sub", Json.createValue("user-42"), ENTITLEMENTS_CLAIM, Json.createValue("ADMIN")));
        QuarkusJwtPrincipalResolver resolver = QuarkusJwtPrincipalResolver.withEntitlementsClaim(
                jwt, ENTITLEMENTS_CLAIM);

        List<String> entitlements = resolver.resolvePrincipal(request).authenticatedClientEntitlements();

        assertThat(entitlements).isNotNull().hasSize(1);
        assertThat(entitlements.getFirst()).isNotNull();
        assertThat(entitlements.getFirst()).isEqualTo("ADMIN");
    }

    // --- fails closed ---

    @Test
    void resolvesNothingWhenTheTokenHasNoClaims() {
        when(jwt.getClaimNames()).thenReturn(Set.of());

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientEntitlements()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void resolvesNothingWhenThereIsNoTokenOnTheRequest() {
        when(jwt.getClaimNames()).thenReturn(null);

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void doesNotTouchTheServletRequest() {
        givenClaims(claims("sub", "user-42"));

        QuarkusJwtPrincipalResolver resolver = new QuarkusJwtPrincipalResolver(jwt);
        resolver.resolvePrincipal(request).authenticatedUserId();
        resolver.resolvePrincipal(request).attributes();

        verifyNoInteractions(request);
    }

}
