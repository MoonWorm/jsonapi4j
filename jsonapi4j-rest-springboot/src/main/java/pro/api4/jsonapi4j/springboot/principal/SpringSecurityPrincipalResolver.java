package pro.api4.jsonapi4j.springboot.principal;

import jakarta.servlet.ServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pro.api4.jsonapi4j.principal.ClaimsPrincipalMapper;
import pro.api4.jsonapi4j.principal.Principal;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * {@link PrincipalResolver} that builds the {@link Principal} from the JWT claims Spring Security has
 * already verified, read from the {@link SecurityContextHolder}.
 * <p>
 * Unlike {@link pro.api4.jsonapi4j.principal.JwtPrincipalResolver}, this resolver never parses a token
 * itself, so it cannot produce a principal from an unverified one: if the resource server rejected the
 * request, or the JSON:API path is not covered by the security filter chain, there is no authentication in
 * the context and no principal is resolved. Authorization then fails closed.
 * <p>
 * Applications opt in by declaring it as a bean, which takes precedence over the framework default:
 * <pre>{@code
 * @Bean
 * public PrincipalResolver jsonapi4jPrincipalResolver(AccessTierRegistry accessTierRegistry) {
 *     return SpringSecurityPrincipalResolver.withAccessTierClaim("access_tier", accessTierRegistry);
 * }
 * }</pre>
 * Requires {@code spring-boot-starter-oauth2-resource-server} on the application classpath, and the
 * JSON:API root path to be authenticated in the application's {@code SecurityFilterChain}.
 * <p>
 * Note that Spring Security converts the registered time claims ({@code exp}, {@code iat}, {@code nbf}) to
 * {@link java.time.Instant}, so those principal attributes carry a different runtime type here than under
 * {@link pro.api4.jsonapi4j.principal.JwtPrincipalResolver}, which exposes them as epoch seconds.
 *
 * @see ClaimsPrincipalMapper
 */
public class SpringSecurityPrincipalResolver implements PrincipalResolver {

    private final ClaimsPrincipalMapper claimsMapper;

    /**
     * Creates a resolver mapping the standard claim names, without access tier resolution.
     */
    public SpringSecurityPrincipalResolver() {
        this(new ClaimsPrincipalMapper());
    }

    /**
     * Creates a resolver with an application-specific claim mapping.
     *
     * @param claimsMapper mapper translating the verified claims into principal values
     */
    public SpringSecurityPrincipalResolver(ClaimsPrincipalMapper claimsMapper) {
        this.claimsMapper = claimsMapper;
    }

    /**
     * Convenience factory for the common case of resolving access tiers from an application-specific claim.
     *
     * @param accessTierClaim    claim holding the access tier name, optionally a dotted path
     * @param accessTierRegistry registry used to resolve the tier name
     * @return a resolver using the standard {@code sub} and {@code scope} claims and the given tier claim
     */
    public static SpringSecurityPrincipalResolver withAccessTierClaim(String accessTierClaim,
                                                                      AccessTierRegistry accessTierRegistry) {
        return new SpringSecurityPrincipalResolver(new ClaimsPrincipalMapper(
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
     * Returns the verified claims of the current authentication.
     *
     * @return the claims, or an empty map when the request carries no JWT authentication
     */
    protected Map<String, Object> resolveClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Collections.emptyMap();
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getClaims();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaims();
        }
        return Collections.emptyMap();
    }

}
