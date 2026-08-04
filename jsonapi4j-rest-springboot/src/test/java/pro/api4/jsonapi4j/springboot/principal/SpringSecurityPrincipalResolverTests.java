package pro.api4.jsonapi4j.springboot.principal;

import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringSecurityPrincipalResolverTests {

    private static final String TIER_CLAIM = "access_tier";

    private final SpringSecurityPrincipalResolver resolver = new SpringSecurityPrincipalResolver();
    private final ServletRequest request = mock(ServletRequest.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256");
        claims.forEach(builder::claim);
        return builder.build();
    }

    private void givenAuthenticatedJwt(Map<String, Object> claims) {
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwtWithClaims(claims)));
    }

    // --- verified claims ---

    @Test
    void resolvesUserIdAndScopesFromVerifiedJwt() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "scope", "read write"));

        assertThat(resolver.resolveUserId(request)).isEqualTo("user-42");
        assertThat(resolver.resolveScopes(request)).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesScopesFromArrayClaim() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "scp", List.of("read", "write")));

        assertThat(resolver.resolveScopes(request)).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesNestedClaimPaths() {
        SpringSecurityPrincipalResolver keycloakResolver = new SpringSecurityPrincipalResolver(
                new pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper(
                        "sub", "realm_access.roles", null, new DefaultAccessTierRegistry()));
        givenAuthenticatedJwt(Map.of("sub", "user-42", "realm_access", Map.of("roles", List.of("admin"))));

        assertThat(keycloakResolver.resolveScopes(request)).containsExactly("admin");
    }

    @Test
    void exposesAllClaimsAsAttributes() {
        givenAuthenticatedJwt(Map.of("sub", "user-42", "email", "user@api4.pro"));

        assertThat(resolver.resolveAttributes(request))
                .containsEntry("sub", "user-42")
                .containsEntry("email", "user@api4.pro");
    }

    @Test
    void resolvesAccessTierFromConfiguredClaim() {
        SpringSecurityPrincipalResolver tierResolver =
                SpringSecurityPrincipalResolver.withAccessTierClaim(TIER_CLAIM, new DefaultAccessTierRegistry());
        givenAuthenticatedJwt(Map.of("sub", "user-42", TIER_CLAIM, "ADMIN"));

        AccessTier tier = tierResolver.resolveAccessTier(request);

        assertThat(tier).isNotNull();
        assertThat(tier.getName()).isEqualTo("ADMIN");
    }

    @Test
    void resolvesClaimsWhenTheAuthenticationPrincipalIsAJwt() {
        Jwt jwt = jwtWithClaims(Map.of("sub", "user-42"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of()));

        assertThat(resolver.resolveUserId(request)).isEqualTo("user-42");
    }

    // --- fails closed ---

    @Test
    void resolvesNothingWhenThereIsNoAuthentication() {
        assertThat(resolver.resolveUserId(request)).isNull();
        assertThat(resolver.resolveScopes(request)).isNull();
        assertThat(resolver.resolveAccessTier(request)).isNull();
        assertThat(resolver.resolveAttributes(request)).isEmpty();
    }

    @Test
    void resolvesNothingForNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "secret", List.of()));

        assertThat(resolver.resolveUserId(request)).isNull();
        assertThat(resolver.resolveScopes(request)).isNull();
        assertThat(resolver.resolveAttributes(request)).isEmpty();
    }

    @Test
    void doesNotTouchTheServletRequest() {
        givenAuthenticatedJwt(Map.of("sub", "user-42"));

        resolver.resolveUserId(request);
        resolver.resolveAttributes(request);

        org.mockito.Mockito.verifyNoInteractions(request);
    }

}
