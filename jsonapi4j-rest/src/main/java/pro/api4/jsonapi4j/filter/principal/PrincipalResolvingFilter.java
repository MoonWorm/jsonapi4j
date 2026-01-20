package pro.api4.jsonapi4j.filter.principal;

import pro.api4.jsonapi4j.plugin.ac.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.plugin.ac.tier.AccessTier;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import pro.api4.jsonapi4j.principal.PrincipalResolver;

import java.io.IOException;
import java.util.Set;

public class PrincipalResolvingFilter implements Filter {

    private final PrincipalResolver resolver;

    public PrincipalResolvingFilter(PrincipalResolver resolver) {
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


