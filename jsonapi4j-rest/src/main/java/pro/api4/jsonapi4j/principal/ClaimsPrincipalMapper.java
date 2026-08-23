package pro.api4.jsonapi4j.principal;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

    /**
     * Claim carrying the entitlements. JWT standardizes no such claim, so this is a jsonapi4j convention:
     * configure your identity provider to emit it, or name your own claim. Pass {@code null} as the
     * entitlements claim to disable entitlements resolution entirely.
     */
    public static final String DEFAULT_ENTITLEMENTS_CLAIM = "entitlements";

    private static final String CLAIM_PATH_SEPARATOR = "\\.";

    private final String userIdClaim;
    private final String scopesClaim;
    private final String entitlementsClaim;

    /**
     * Creates a mapper using the default claim names, including the default entitlements claim.
     */
    public ClaimsPrincipalMapper() {
        this(DEFAULT_USER_ID_CLAIM, DEFAULT_SCOPES_CLAIM, DEFAULT_ENTITLEMENTS_CLAIM);
    }

    /**
     * Creates a mapper with explicit claim names.
     *
     * @param userIdClaim       claim holding the user id, e.g. {@code sub} or {@code oid}
     * @param scopesClaim       claim holding the granted scopes, accepted either as a space-delimited
     *                          string or as an array of strings
     * @param entitlementsClaim claim holding the entitlements, accepted either as a space-delimited
     *                          string or as an array of strings; {@code null} disables entitlements
     *                          resolution
     */
    public ClaimsPrincipalMapper(String userIdClaim,
                                 String scopesClaim,
                                 String entitlementsClaim) {
        this.userIdClaim = userIdClaim;
        this.scopesClaim = scopesClaim;
        this.entitlementsClaim = entitlementsClaim;
    }

    /**
     * Creates a mapper with explicit claim names for userId and scopes, leaving entitlements unresolved.
     *
     * @param userIdClaim claim holding the user id, e.g. {@code sub} or {@code oid}
     * @param scopesClaim claim holding the granted scopes, accepted either as a space-delimited
     *                    string or as an array of strings
     */
    public ClaimsPrincipalMapper(String userIdClaim,
                                 String scopesClaim) {
        this(userIdClaim, scopesClaim, null);
    }

    /**
     * Creates a mapper with default claim names except for the entitlements claim.
     *
     * @param entitlementsClaim claim holding the entitlements, accepted either as a space-delimited
     *                          string or as an array of strings; {@code null} disables entitlements
     *                          resolution
     */
    public ClaimsPrincipalMapper(String entitlementsClaim) {
        this(DEFAULT_USER_ID_CLAIM, DEFAULT_SCOPES_CLAIM, entitlementsClaim);
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
     * Resolves the entitlements from the configured entitlements claim. For multiple values accepts the same format
     * as for scopes - space-separated string.
     * <p>
     * JWT defines no standard claim for entitlements, so this returns {@code null} unless an
     * application-specific claim name has been configured.
     *
     * @param claims verified JWT claims
     * @return the resolved list of entitlements, or {@code null} if no entitlements claim is configured
     * or the claim is absent
     */
    public List<String> resolveEntitlements(Map<String, Object> claims) {
        if (entitlementsClaim == null) {
            return null;
        }
        Object value = readClaim(claims, entitlementsClaim);
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        String scopes = String.valueOf(value);
        if (StringUtils.isBlank(scopes)) {
            return Collections.emptyList();
        }
        return Arrays.stream(scopes.trim().split("\\s+")).toList();
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
     * Auth0's {@code https://example.com/entitlements} convention — resolve as-is. Only when no such claim
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
