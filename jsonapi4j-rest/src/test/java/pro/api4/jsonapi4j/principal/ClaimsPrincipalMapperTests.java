package pro.api4.jsonapi4j.principal;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimsPrincipalMapperTests {

    private static final String TIER_CLAIM = "https://api4.pro/access_tier";

    private final ClaimsPrincipalMapper mapper = new ClaimsPrincipalMapper();

    // --- user id ---

    @Test
    void resolvesUserIdFromSubClaim() {
        assertThat(mapper.resolveUserId(Map.of("sub", "user-42"))).isEqualTo("user-42");
    }

    @Test
    void returnsNullUserIdWhenSubClaimIsAbsent() {
        assertThat(mapper.resolveUserId(Map.of("scope", "read"))).isNull();
    }

    @Test
    void resolvesUserIdFromCustomClaim() {
        ClaimsPrincipalMapper oidMapper = new ClaimsPrincipalMapper(
                "oid", ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM, null, new DefaultAccessTierRegistry());

        assertThat(oidMapper.resolveUserId(Map.of("sub", "ignored", "oid", "azure-id"))).isEqualTo("azure-id");
    }

    // --- scopes: the shape matrix ---

    @Test
    void resolvesSpaceDelimitedScopeClaim() {
        assertThat(mapper.resolveScopes(Map.of("scope", "read write admin")))
                .containsExactlyInAnyOrder("read", "write", "admin");
    }

    @Test
    void resolvesArrayScopeClaim() {
        assertThat(mapper.resolveScopes(Map.of("scope", List.of("read", "write"))))
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void fallsBackToScpClaimAsSpaceDelimitedString() {
        assertThat(mapper.resolveScopes(Map.of("scp", "read write")))
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void fallsBackToScpClaimAsArray() {
        assertThat(mapper.resolveScopes(Map.of("scp", List.of("read", "write"))))
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void prefersScopeClaimOverScpFallback() {
        assertThat(mapper.resolveScopes(Map.of("scope", "read", "scp", "admin")))
                .containsExactly("read");
    }

    @Test
    void doesNotFallBackToScpWhenScopesClaimIsCustomized() {
        ClaimsPrincipalMapper customMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM, "permissions", null, new DefaultAccessTierRegistry());

        assertThat(customMapper.resolveScopes(Map.of("scp", "read"))).isNull();
        assertThat(customMapper.resolveScopes(Map.of("permissions", "read"))).containsExactly("read");
    }

    @Test
    void collapsesRepeatedWhitespaceBetweenScopes() {
        assertThat(mapper.resolveScopes(Map.of("scope", "  read   write  ")))
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void returnsEmptySetForBlankScopeClaim() {
        assertThat(mapper.resolveScopes(Map.of("scope", "   "))).isEmpty();
    }

    @Test
    void returnsNullWhenNoScopeClaimIsPresent() {
        assertThat(mapper.resolveScopes(Map.of("sub", "user-42"))).isNull();
    }

    // --- scopes: nested claim paths ---

    @Test
    void resolvesScopesFromNestedClaimPath() {
        ClaimsPrincipalMapper keycloakMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                "realm_access.roles",
                null,
                new DefaultAccessTierRegistry());

        Map<String, Object> claims = Map.of("realm_access", Map.of("roles", List.of("read", "write")));

        assertThat(keycloakMapper.resolveScopes(claims)).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesDeeplyNestedClaimPath() {
        ClaimsPrincipalMapper keycloakMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                "resource_access.jsonapi4j.roles",
                null,
                new DefaultAccessTierRegistry());

        Map<String, Object> claims = Map.of(
                "resource_access", Map.of("jsonapi4j", Map.of("roles", List.of("admin"))));

        assertThat(keycloakMapper.resolveScopes(claims)).containsExactly("admin");
    }

    @Test
    void prefersLiteralClaimNameOverPathWalkingForNamespacedClaims() {
        ClaimsPrincipalMapper auth0Mapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                "https://api4.pro/roles",
                null,
                new DefaultAccessTierRegistry());

        Map<String, Object> claims = Map.of("https://api4.pro/roles", List.of("read", "write"));

        assertThat(auth0Mapper.resolveScopes(claims)).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void returnsNullWhenNestedClaimPathIsBroken() {
        ClaimsPrincipalMapper keycloakMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                "realm_access.roles",
                null,
                new DefaultAccessTierRegistry());

        assertThat(keycloakMapper.resolveScopes(Map.of("realm_access", "not-an-object"))).isNull();
        assertThat(keycloakMapper.resolveScopes(Map.of("realm_access", Map.of("other", "x")))).isNull();
        assertThat(keycloakMapper.resolveScopes(Map.of("sub", "user-42"))).isNull();
    }

    // --- access tier ---

    @Test
    void doesNotResolveAccessTierWhenNoClaimIsConfigured() {
        assertThat(mapper.resolveAccessTier(Map.of("access_tier", "ADMIN"))).isNull();
    }

    @Test
    void resolvesAccessTierFromConfiguredClaim() {
        ClaimsPrincipalMapper tierMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM,
                TIER_CLAIM,
                new DefaultAccessTierRegistry());

        AccessTier tier = tierMapper.resolveAccessTier(Map.of(TIER_CLAIM, "ADMIN"));

        assertThat(tier).isNotNull();
        assertThat(tier.getName()).isEqualTo("ADMIN");
    }

    @Test
    void fallsBackToDefaultTierForUnknownTierName() {
        ClaimsPrincipalMapper tierMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM,
                TIER_CLAIM,
                new DefaultAccessTierRegistry());

        AccessTier tier = tierMapper.resolveAccessTier(Map.of(TIER_CLAIM, "NOT_A_REGISTERED_TIER"));

        assertThat(tier).isNotNull();
        assertThat(tier.getName()).isEqualTo("PUBLIC");
    }

    @Test
    void returnsNullAccessTierWhenConfiguredClaimIsAbsent() {
        ClaimsPrincipalMapper tierMapper = new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM,
                TIER_CLAIM,
                new DefaultAccessTierRegistry());

        assertThat(tierMapper.resolveAccessTier(Map.of("sub", "user-42"))).isNull();
    }

    // --- attributes ---

    @Test
    void exposesAllClaimsAsAttributes() {
        Map<String, Object> claims = Map.of("sub", "user-42", "email", "user@api4.pro", "exp", 1893456000);

        assertThat(mapper.resolveAttributes(claims)).containsExactlyInAnyOrderEntriesOf(claims);
    }

    @Test
    void returnsUnmodifiableAttributes() {
        Map<String, Object> attributes = mapper.resolveAttributes(new java.util.HashMap<>(Map.of("sub", "user-42")));

        assertThatThrownBy(() -> attributes.put("injected", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void returnsEmptyAttributesForNullClaims() {
        assertThat(mapper.resolveAttributes(null)).isEmpty();
    }

    // --- null claim safety ---

    @Test
    void toleratesNullClaims() {
        assertThat(mapper.resolveUserId(null)).isNull();
        assertThat(mapper.resolveScopes(null)).isNull();
        assertThat(mapper.resolveAccessTier(null)).isNull();
    }

}
