package pro.api4.jsonapi4j.filter.principal;

import jakarta.servlet.*;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.io.IOException;
import java.util.Set;

public class PrincipalResolvingFilter implements Filter {

    public static final String PRINCIPAL_RESOLVER_ATT_NAME = "jsonapi4jPrincipalResolver";

    private PrincipalResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        resolver = (PrincipalResolver) filterConfig.getServletContext().getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (resolver == null) {
            resolver = new DefaultPrincipalResolver();
        }
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


