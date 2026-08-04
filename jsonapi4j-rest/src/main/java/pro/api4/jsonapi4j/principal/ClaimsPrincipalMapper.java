package pro.api4.jsonapi4j.principal;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps a set of JWT claims onto the values that make up a {@link Principal}.
 * <p>
 * This mapper performs <strong>no</strong> token validation — it assumes the claims it receives have
 * already been verified (signature, {@code exp}, {@code iss}, {@code aud}) by the surrounding security
 * layer, e.g. Spring Security's resource server, Quarkus OIDC, or an API gateway. It exists purely so
 * that the same claim-to-principal mapping can be shared between the framework-native resolvers and
 * {@link JwtPrincipalResolver}.
 * <p>
 * Claim names are configurable and support dotted paths for claims nested inside JSON objects,
 * e.g. {@code realm_access.roles} as used by Keycloak.
 *
 * @see JwtPrincipalResolver
 */
public class ClaimsPrincipalMapper {

    /**
     * Subject claim as defined by RFC 7519.
     */
    public static final String DEFAULT_USER_ID_CLAIM = "sub";

    /**
     * Scope claim as defined by RFC 9068, used by Keycloak and Auth0 among others.
     */
    public static final String DEFAULT_SCOPES_CLAIM = "scope";

    /**
     * Scope claim used by Azure AD (space-delimited string) and Okta (JSON array).
     * Consulted only when {@link #DEFAULT_SCOPES_CLAIM} is in use and yields no value.
     */
    public static final String FALLBACK_SCOPES_CLAIM = "scp";

    private static final String CLAIM_PATH_SEPARATOR = "\\.";

    private final String userIdClaim;
    private final String scopesClaim;
    private final String accessTierClaim;
    private final AccessTierRegistry accessTierRegistry;

    /**
     * Creates a mapper using the standard claim names and the default access tier registry.
     * Access tiers are not resolved, since no JWT claim carries them by convention.
     */
    public ClaimsPrincipalMapper() {
        this(DEFAULT_USER_ID_CLAIM, DEFAULT_SCOPES_CLAIM, null, new DefaultAccessTierRegistry());
    }

    /**
     * Creates a mapper with explicit claim names.
     *
     * @param userIdClaim        claim holding the user id, e.g. {@code sub} or {@code oid}
     * @param scopesClaim        claim holding the granted scopes, accepted either as a space-delimited
     *                           string or as an array of strings
     * @param accessTierClaim    claim holding the access tier name, resolved through
     *                           {@code accessTierRegistry}; {@code null} disables access tier resolution
     * @param accessTierRegistry registry used to look up the tier name found in {@code accessTierClaim}
     */
    public ClaimsPrincipalMapper(String userIdClaim,
                                 String scopesClaim,
                                 String accessTierClaim,
                                 AccessTierRegistry accessTierRegistry) {
        this.userIdClaim = userIdClaim;
        this.scopesClaim = scopesClaim;
        this.accessTierClaim = accessTierClaim;
        this.accessTierRegistry = accessTierRegistry;
    }

    /**
     * Resolves the user id from the configured user id claim.
     *
     * @param claims verified JWT claims
     * @return the user id, or {@code null} if the claim is absent
     */
    public String resolveUserId(Map<String, Object> claims) {
        Object value = readClaim(claims, userIdClaim);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * Resolves the granted scopes from the configured scopes claim, accepting both shapes found in the
     * wild: a space-delimited string ({@code "read write"}) and an array of strings
     * ({@code ["read", "write"]}). When the default {@code scope} claim is configured and absent,
     * {@code scp} is consulted as a fallback.
     *
     * @param claims verified JWT claims
     * @return the granted scopes, an empty set if the claim is present but blank,
     * or {@code null} if the claim is absent
     */
    public Set<String> resolveScopes(Map<String, Object> claims) {
        Object value = readClaim(claims, scopesClaim);
        if (value == null && DEFAULT_SCOPES_CLAIM.equals(scopesClaim)) {
            value = readClaim(claims, FALLBACK_SCOPES_CLAIM);
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
        }
        String scopes = String.valueOf(value);
        if (StringUtils.isBlank(scopes)) {
            return Collections.emptySet();
        }
        return Arrays.stream(scopes.trim().split("\\s+")).collect(Collectors.toSet());
    }

    /**
     * Resolves the access tier from the configured access tier claim.
     * <p>
     * JWT defines no standard claim for access tiers, so this returns {@code null} unless an
     * application-specific claim name has been configured.
     *
     * @param claims verified JWT claims
     * @return the resolved {@link AccessTier}, or {@code null} if no access tier claim is configured
     * or the claim is absent
     */
    public AccessTier resolveAccessTier(Map<String, Object> claims) {
        if (accessTierClaim == null) {
            return null;
        }
        Object value = readClaim(claims, accessTierClaim);
        if (value == null) {
            return null;
        }
        return accessTierRegistry.getAccessTierOrDefault(String.valueOf(value));
    }

    /**
     * Exposes the full claim set as principal attributes.
     *
     * @param claims verified JWT claims
     * @return an unmodifiable view of the claims, never {@code null}
     */
    public Map<String, Object> resolveAttributes(Map<String, Object> claims) {
        if (claims == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(claims);
    }

    /**
     * Reads a claim by name.
     * <p>
     * The name is first looked up literally, so that namespaced claim names containing dots — such as
     * Auth0's {@code https://example.com/access_tier} convention — resolve as-is. Only when no such claim
     * exists is the name treated as a dot-separated path and walked through nested JSON objects, which is
     * what makes Keycloak's {@code realm_access.roles} resolvable.
     *
     * @param claims    verified JWT claims
     * @param claimPath claim name, optionally a dot-separated path into nested claims
     * @return the claim value, or {@code null} if the claim is absent
     */
    @SuppressWarnings("unchecked")
    private Object readClaim(Map<String, Object> claims, String claimPath) {
        if (claims == null || StringUtils.isBlank(claimPath)) {
            return null;
        }
        Object literal = claims.get(claimPath);
        if (literal != null) {
            return literal;
        }
        String[] segments = claimPath.split(CLAIM_PATH_SEPARATOR);
        if (segments.length < 2) {
            return null;
        }
        Object current = claims;
        for (String segment : segments) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(segment);
        }
        return current;
    }

}
