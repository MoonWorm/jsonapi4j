package io.jsonapi4j.servlet.filter.ac;

import io.jsonapi4j.plugin.ac.AuthenticatedPrincipalContextHolder;
import io.jsonapi4j.plugin.ac.tier.AccessTier;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.Set;

public class JsonApi4jAccessControlFilter implements Filter {

    private final PrincipalResolver resolver;

    public JsonApi4jAccessControlFilter(PrincipalResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        AccessTier accessTierName = resolver.resolveAccessTier(servletRequest);
        Set<String> scopes = resolver.resolveScopes(servletRequest);
        String userId = resolver.resolveUserId(servletRequest);

        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                accessTierName,
                scopes,
                userId
        );

        filterChain.doFilter(servletRequest, servletResponse);
    }

}


