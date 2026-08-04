package pro.api4.jsonapi4j.principal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * {@link PrincipalResolver} that builds the {@link Principal} from the claims of the JWT carried in the
 * {@code Authorization: Bearer ...} header.
 * <p>
 * <strong>This resolver does not validate the token.</strong> It decodes the payload segment and maps the
 * claims through {@link ClaimsPrincipalMapper}, assuming the surrounding security layer — Spring Security's
 * resource server, Quarkus OIDC, or an upstream API gateway — has already verified the signature and the
 * standard claims, and rejects requests carrying an invalid token before they reach the JSON:API servlet.
 * Make sure that layer is configured to protect the JSON:API root path; otherwise a forged token would be
 * mapped into an authenticated principal.
 * <p>
 * The decoded claims are cached as a request attribute so that a single request decodes the token once.
 *
 * @see ClaimsPrincipalMapper
 * @see DefaultPrincipalResolver
 */
@Slf4j
public class JwtPrincipalResolver implements PrincipalResolver {

    public static final String DEFAULT_AUTHORIZATION_HEADER_NAME = "Authorization";

    /**
     * Request attribute under which the decoded claims are cached, so that the four resolver calls made
     * per request by {@code PrincipalResolvingFilter} decode the token once.
     */
    static final String CLAIMS_REQUEST_ATT_NAME = "pro.api4.jsonapi4j.principal.jwt-claims";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int JWT_PAYLOAD_SEGMENT = 1;
    private static final int JWT_SEGMENTS = 3;

    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final String authorizationHeaderName;
    private final ClaimsPrincipalMapper claimsMapper;
    private final ObjectMapper jsonMapper;

    /**
     * Creates a resolver reading the {@code Authorization} header and mapping the standard claim names.
     */
    public JwtPrincipalResolver() {
        this(new ClaimsPrincipalMapper());
    }

    /**
     * Creates a resolver reading the {@code Authorization} header with an application-specific claim mapping.
     *
     * @param claimsMapper mapper translating the decoded claims into principal values
     */
    public JwtPrincipalResolver(ClaimsPrincipalMapper claimsMapper) {
        this(DEFAULT_AUTHORIZATION_HEADER_NAME, claimsMapper, new ObjectMapper());
    }

    /**
     * Creates a resolver with an application-specific claim mapping, reading the token from a custom header.
     *
     * @param authorizationHeaderName header carrying the bearer token, e.g. {@code X-Forwarded-Access-Token}
     * @param claimsMapper            mapper translating the decoded claims into principal values
     * @param jsonMapper              mapper used to deserialize the decoded JWT payload
     */
    public JwtPrincipalResolver(String authorizationHeaderName,
                                ClaimsPrincipalMapper claimsMapper,
                                ObjectMapper jsonMapper) {
        this.authorizationHeaderName = authorizationHeaderName;
        this.claimsMapper = claimsMapper;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Convenience factory for the common case of resolving access tiers from an application-specific claim.
     *
     * @param accessTierClaim    claim holding the access tier name, optionally a dotted path
     * @param accessTierRegistry registry used to resolve the tier name
     * @return a resolver using the standard {@code sub} and {@code scope} claims and the given tier claim
     */
    public static JwtPrincipalResolver withAccessTierClaim(String accessTierClaim,
                                                           AccessTierRegistry accessTierRegistry) {
        return new JwtPrincipalResolver(new ClaimsPrincipalMapper(
                ClaimsPrincipalMapper.DEFAULT_USER_ID_CLAIM,
                ClaimsPrincipalMapper.DEFAULT_SCOPES_CLAIM,
                accessTierClaim,
                accessTierRegistry
        ));
    }

    @Override
    public AccessTier resolveAccessTier(ServletRequest servletRequest) {
        return claimsMapper.resolveAccessTier(resolveClaims(servletRequest));
    }

    @Override
    public Set<String> resolveScopes(ServletRequest servletRequest) {
        return claimsMapper.resolveScopes(resolveClaims(servletRequest));
    }

    @Override
    public String resolveUserId(ServletRequest servletRequest) {
        return claimsMapper.resolveUserId(resolveClaims(servletRequest));
    }

    @Override
    public Map<String, Object> resolveAttributes(ServletRequest servletRequest) {
        return claimsMapper.resolveAttributes(resolveClaims(servletRequest));
    }

    /**
     * Returns the claims of the request's bearer token, decoding them on first access and caching the
     * result under {@link #CLAIMS_REQUEST_ATT_NAME}.
     *
     * @param servletRequest the current request
     * @return the decoded claims, or an empty map when no readable token is present
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> resolveClaims(ServletRequest servletRequest) {
        Object cached = servletRequest.getAttribute(CLAIMS_REQUEST_ATT_NAME);
        if (cached != null) {
            return (Map<String, Object>) cached;
        }
        Map<String, Object> claims = decodeClaims(readBearerToken(servletRequest));
        servletRequest.setAttribute(CLAIMS_REQUEST_ATT_NAME, claims);
        return claims;
    }

    /**
     * Extracts the bearer token from the configured authorization header.
     *
     * @param servletRequest the current request
     * @return the raw token, or {@code null} when the header is absent or is not a bearer token
     */
    private String readBearerToken(ServletRequest servletRequest) {
        if (!(servletRequest instanceof HttpServletRequest httpRequest)) {
            return null;
        }
        String headerValue = httpRequest.getHeader(authorizationHeaderName);
        if (StringUtils.isBlank(headerValue)) {
            return null;
        }
        if (!StringUtils.startsWithIgnoreCase(headerValue, BEARER_PREFIX)) {
            log.debug(
                    "Header {} does not carry a bearer token. No principal claims will be resolved.",
                    authorizationHeaderName
            );
            return null;
        }
        return StringUtils.trimToNull(headerValue.substring(BEARER_PREFIX.length()));
    }

    /**
     * Decodes the payload segment of a JWT into its claims.
     *
     * @param token the raw bearer token
     * @return the decoded claims, or an empty map when the token is absent or cannot be decoded
     */
    private Map<String, Object> decodeClaims(String token) {
        if (token == null) {
            return Collections.emptyMap();
        }
        String[] segments = token.split("\\.");
        if (segments.length != JWT_SEGMENTS) {
            log.debug("Bearer token is not a well-formed JWT: expected {} segments, got {}.",
                    JWT_SEGMENTS, segments.length);
            return Collections.emptyMap();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(segments[JWT_PAYLOAD_SEGMENT]);
            Map<String, Object> claims = jsonMapper.readValue(payload, CLAIMS_TYPE);
            return claims != null ? claims : Collections.emptyMap();
        } catch (Exception ex) {
            log.debug("Failed to decode the JWT payload. No principal claims will be resolved.", ex);
            return Collections.emptyMap();
        }
    }

}
