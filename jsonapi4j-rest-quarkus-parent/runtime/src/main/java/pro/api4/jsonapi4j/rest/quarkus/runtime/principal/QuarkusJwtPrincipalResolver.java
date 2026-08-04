package pro.api4.jsonapi4j.rest.quarkus.runtime.principal;

import jakarta.servlet.ServletRequest;
import org.eclipse.microprofile.jwt.JsonWebToken;
import pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper;
import pro.api4.jsonapi4j.principal.Principal;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link PrincipalResolver} that builds the {@link Principal} from the claims Quarkus has already verified,
 * read from the injected {@link JsonWebToken}.
 * <p>
 * Unlike {@link pro.api4.jsonapi4j.principal.JwtPrincipalResolver}, this resolver never parses a token
 * itself, so it cannot produce a principal from an unverified one: when the request carries no valid token,
 * the token has no claims and no principal is resolved. Authorization then fails closed.
 * <p>
 * Applications opt in by producing it as a bean, which overrides the extension's {@code @DefaultBean}:
 * <pre>{@code
 * @Produces
 * @Singleton
 * PrincipalResolver principalResolver(JsonWebToken jwt, AccessTierRegistry accessTierRegistry) {
 *     return QuarkusJwtPrincipalResolver.withAccessTierClaim(jwt, "access_tier", accessTierRegistry);
 * }
 * }</pre>
 * Requires {@code quarkus-oidc} or {@code quarkus-smallrye-jwt} on the application classpath, and the
 * JSON:API root path to be authenticated (e.g. via {@code quarkus.http.auth.permission.*}).
 * <p>
 * The {@link JsonWebToken} passed in is the request-scoped CDI client proxy, so a single resolver instance
 * serves every request and always sees the current request's claims.
 *
 * @see ClaimsPrincipalMapper
 */
public class QuarkusJwtPrincipalResolver implements PrincipalResolver {

    private final JsonWebToken jwt;
    private final ClaimsPrincipalMapper claimsMapper;

    /**
     * Creates a resolver mapping the standard claim names, without access tier resolution.
     *
     * @param jwt the injected request-scoped token
     */
    public QuarkusJwtPrincipalResolver(JsonWebToken jwt) {
        this(jwt, new ClaimsPrincipalMapper());
    }

    /**
     * Creates a resolver with an application-specific claim mapping.
     *
     * @param jwt          the injected request-scoped token
     * @param claimsMapper mapper translating the verified claims into principal values
     */
    public QuarkusJwtPrincipalResolver(JsonWebToken jwt, ClaimsPrincipalMapper claimsMapper) {
        this.jwt = jwt;
        this.claimsMapper = claimsMapper;
    }

    /**
     * Convenience factory for the common case of resolving access tiers from an application-specific claim.
     *
     * @param jwt                the injected request-scoped token
     * @param accessTierClaim    claim holding the access tier name, optionally a dotted path
     * @param accessTierRegistry registry used to resolve the tier name
     * @return a resolver using the standard {@code sub} and {@code scope} claims and the given tier claim
     */
    public static QuarkusJwtPrincipalResolver withAccessTierClaim(JsonWebToken jwt,
                                                                  String accessTierClaim,
                                                                  AccessTierRegistry accessTierRegistry) {
        return new QuarkusJwtPrincipalResolver(jwt, new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM,
                accessTierClaim,
                accessTierRegistry
        ));
    }

    @Override
    public AccessTier resolveAccessTier(ServletRequest servletRequest) {
        return claimsMapper.resolveAccessTier(resolveClaims());
    }

    @Override
    public Set<String> resolveScopes(ServletRequest servletRequest) {
        return claimsMapper.resolveScopes(resolveClaims());
    }

    @Override
    public String resolveUserId(ServletRequest servletRequest) {
        return claimsMapper.resolveUserId(resolveClaims());
    }

    @Override
    public Map<String, Object> resolveAttributes(ServletRequest servletRequest) {
        return claimsMapper.resolveAttributes(resolveClaims());
    }

    /**
     * Returns the verified claims of the current request's token, normalized from the JSON-P types
     * MicroProfile JWT exposes for custom claims into plain Java types.
     *
     * @return the claims, or an empty map when the request carries no token
     */
    protected Map<String, Object> resolveClaims() {
        Set<String> claimNames = jwt != null ? jwt.getClaimNames() : null;
        if (claimNames == null || claimNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> claims = new HashMap<>();
        for (String claimName : claimNames) {
            claims.put(claimName, MicroProfileClaims.toJavaType(jwt.getClaim(claimName)));
        }
        return claims;
    }

}
