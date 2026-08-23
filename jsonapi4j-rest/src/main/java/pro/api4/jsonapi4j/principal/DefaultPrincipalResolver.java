package pro.api4.jsonapi4j.principal;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultPrincipalResolver implements PrincipalResolver {

    public static final String DEFAULT_ENTITLEMENTS_HEADER_NAME = "X-Authenticated-Client-Entitlements";
    public static final String DEFAULT_SCOPES_HEADER_NAME = "X-Authenticated-User-Granted-Scopes";
    public static final String DEFAULT_USER_ID_HEADER_NAME = "X-Authenticated-User-Id";

    private String entitlementsHttpHeaderName = DEFAULT_ENTITLEMENTS_HEADER_NAME;
    private String scopesHttpHeaderName = DEFAULT_SCOPES_HEADER_NAME;
    private String userIdHttpHeaderName = DEFAULT_USER_ID_HEADER_NAME;

    public DefaultPrincipalResolver(String entitlementsHttpHeaderName,
                                    String scopesHttpHeaderName,
                                    String userIdHttpHeaderName) {
        this.entitlementsHttpHeaderName = entitlementsHttpHeaderName;
        this.scopesHttpHeaderName = scopesHttpHeaderName;
        this.userIdHttpHeaderName = userIdHttpHeaderName;
    }

    public DefaultPrincipalResolver() {
    }

    @Override
    public Principal resolvePrincipal(ServletRequest servletRequest) {
        return new DefaultPrincipal(
                resolveEntitlements(servletRequest),
                resolveScopes(servletRequest),
                resolveUserId(servletRequest),
                Map.of()
        );
    }

    private List<String> resolveEntitlements(ServletRequest servletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String headerValue = httpRequest.getHeader(entitlementsHttpHeaderName);
        if (headerValue != null) {
            if (StringUtils.isBlank(headerValue)) {
                return Collections.emptyList();
            }
            return Arrays.stream(headerValue.trim().split("\\s+")).toList();
        } else {
            return null;
        }
    }

    private Set<String> resolveScopes(ServletRequest servletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String headerValue = httpRequest.getHeader(scopesHttpHeaderName);
        if (headerValue != null) {
            if (StringUtils.isBlank(headerValue)) {
                return Collections.emptySet();
            }
            return Arrays.stream(headerValue.trim().split("\\s+")).collect(Collectors.toSet());
        }
        return null;
    }

    /**
     * Reads the caller's id from the configured header, normalizing a blank header to {@code null}.
     * <p>
     * A gateway that forwards the header with an empty value rather than omitting it must not produce a
     * principal that counts as authenticated, so blank is treated exactly like an absent header.
     *
     * @param servletRequest the current request
     * @return the authenticated user id, or {@code null} if the header is absent or blank
     */
    private String resolveUserId(ServletRequest servletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String headerValue = httpRequest.getHeader(userIdHttpHeaderName);
        return StringUtils.isBlank(headerValue) ? null : headerValue.trim();
    }

}
