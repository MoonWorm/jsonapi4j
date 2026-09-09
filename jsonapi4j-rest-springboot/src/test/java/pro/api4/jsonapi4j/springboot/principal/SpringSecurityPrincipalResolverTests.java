package pro.api4.jsonapi4j.springboot.principal;

import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringSecurityPrincipalResolverTests {

    private static final String ENTITLEMENTS_CLAIM = "entitlements";

    private final SpringSecurityPrincipalResolver resolver = new SpringSecurityPrincipalResolver();
    private final ServletRequest request = mock(ServletRequest.class);

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenAuthenticatedJwt(Map<String, Object> claims) {
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwtWithClaims(claims)));
    }

    // --- verified claims ---

    @Test
    void resolvesUserIdAndScopesFromVerifiedJwt() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "scope", "read write"));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesScopesFromArrayClaim() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "scp", List.of("read", "write")));

        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesNestedClaimPaths() {
        SpringSecurityPrincipalResolver keycloakResolver = new SpringSecurityPrincipalResolver(
                new pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper(
                        "sub", "realm_access.roles", null));
        givenAuthenticatedJwt(Map.of("sub", "user-42", "realm_access", Map.of("roles", List.of("admin"))));

        assertThat(keycloakResolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactly("admin");
    }

    @Test
    void exposesAllClaimsAsAttributes() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "email", "user@api4.pro"));

        assertThat(resolver.resolvePrincipal(request).attributes())
                .containsEntry("sub", "user-42")
                .containsEntry("email", "user@api4.pro");
    }

    @Test
    void resolvesEntitlementsFromConfiguredClaim() {
        SpringSecurityPrincipalResolver entitlementsResolver =
                SpringSecurityPrincipalResolver.withEntitlementsClaim(ENTITLEMENTS_CLAIM);
        givenAuthenticatedJwt(Map.of("sub", "user-42", ENTITLEMENTS_CLAIM, "ADMIN"));

        List<String> entitlements = entitlementsResolver.resolvePrincipal(request).authenticatedClientEntitlements();
        assertThat(entitlements).isNotNull().hasSize(1);
        assertThat(entitlements.getFirst()).isNotNull().isEqualTo("ADMIN");
    }

    @Test
    void resolvesClaimsWhenTheAuthenticationPrincipalIsAJwt() {
        Jwt jwt = jwtWithClaims(Map.of("sub", "user-42"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of()));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
    }

    // --- fails closed ---

    @Test
    void resolvesNothingWhenThereIsNoAuthentication() {
        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientEntitlements()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void resolvesNothingForAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientEntitlements()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void resolvesNothingForNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "secret", List.of()));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void doesNotTouchTheServletRequest() {
        givenAuthenticatedJwt(Map.of("sub", "user-42"));

        resolver.resolvePrincipal(request).authenticatedUserId();
        resolver.resolvePrincipal(request).attributes();

        org.mockito.Mockito.verifyNoInteractions(request);
    }

}
