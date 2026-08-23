package pro.api4.jsonapi4j.rest.quarkus.runtime.principal;

import jakarta.servlet.ServletRequest;
import org.eclipse.microprofile.jwt.JsonWebToken;
import pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;
import pro.api4.jsonapi4j.principal.Principal;
import pro.api4.jsonapi4j.principal.PrincipalResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * PrincipalResolver principalResolver(JsonWebToken jwt) {
 *     return QuarkusJwtPrincipalResolver.withEntitlementsClaim(jwt, "entitlements");
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
     * Creates a resolver mapping the standard claim names, without entitlements resolution.
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
     * Convenience factory for the common case of resolving entitlements from an application-specific claim.
     *
     * @param jwt             the injected request-scoped token
     * @param entitlementsClaim claim holding the entitlements, optionally a dotted path
     * @return a resolver using the standard {@code sub} and {@code scope} claims and the given entitlements claim
     */
    public static QuarkusJwtPrincipalResolver withEntitlementsClaim(JsonWebToken jwt,
                                                                  String entitlementsClaim) {
        return new QuarkusJwtPrincipalResolver(jwt, new ClaimsPrincipalMapper(entitlementsClaim));
    }

    @Override
    public Principal resolvePrincipal(ServletRequest servletRequest) {
        Map<String, Object> claims = resolveClaims();
        return new DefaultPrincipal(
                claimsMapper.resolveEntitlements(claims),
                claimsMapper.resolveScopes(claims),
                claimsMapper.resolveUserId(claims),
                claimsMapper.resolveAttributes(claims)
        );
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
