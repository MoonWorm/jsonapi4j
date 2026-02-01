package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultPrincipalResolver implements PrincipalResolver {

    public static final String DEFAULT_ACCESS_TIER_HEADER_NAME = "X-Authenticated-Client-Access-Tier";
    public static final String DEFAULT_SCOPES_HEADER_NAME = "X-Authenticated-User-Granted-Scopes";
    public static final String DEFAULT_USER_ID_HEADER_NAME = "X-Authenticated-User-Id";

    private String accessTierHttpHeaderName = DEFAULT_ACCESS_TIER_HEADER_NAME;
    private String scopesHttpHeaderName = DEFAULT_SCOPES_HEADER_NAME;
    private String userIdHttpHeaderName = DEFAULT_USER_ID_HEADER_NAME;

    private AccessTierRegistry accessTierRegistry = new DefaultAccessTierRegistry();

    public DefaultPrincipalResolver(String accessTierHttpHeaderName,
                                    String scopesHttpHeaderName,
                                    String userIdHttpHeaderName,
                                    AccessTierRegistry accessTierRegistry) {
        this.accessTierHttpHeaderName = accessTierHttpHeaderName;
        this.scopesHttpHeaderName = scopesHttpHeaderName;
        this.userIdHttpHeaderName = userIdHttpHeaderName;
        this.accessTierRegistry = accessTierRegistry;
    }

    public DefaultPrincipalResolver(AccessTierRegistry accessTierRegistry) {
        this.accessTierRegistry = accessTierRegistry;
    }

    public DefaultPrincipalResolver() {
    }

    @Override
    public AccessTier resolveAccessTier(ServletRequest servletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String headerValue = httpRequest.getHeader(accessTierHttpHeaderName);
        if (headerValue != null) {
            return accessTierRegistry.getAccessTierOrDefault(headerValue);
        } else {
            return null;
        }
    }

    @Override
    public Set<String> resolveScopes(ServletRequest servletRequest) {
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

    @Override
    public String resolveUserId(ServletRequest servletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        return httpRequest.getHeader(userIdHttpHeaderName);
    }

}
