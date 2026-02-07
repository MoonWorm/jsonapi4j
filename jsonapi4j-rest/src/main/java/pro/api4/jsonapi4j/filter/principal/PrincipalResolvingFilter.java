package pro.api4.jsonapi4j.filter.principal;

import jakarta.servlet.*;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTier;

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
                new DefaultPrincipal(
                        accessTierName,
                        scopes,
                        userId
                )
        );

        filterChain.doFilter(servletRequest, servletResponse);
    }

}


