package pro.api4.jsonapi4j.filter.principal;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class PrincipalResolvingFilter implements Filter {

    public static final String PRINCIPAL_RESOLVER_ATT_NAME = "jsonapi4jPrincipalResolver";

    private PrincipalResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        resolver = initJsonApi4jPrincipalResolver(filterConfig.getServletContext());
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

    private PrincipalResolver initJsonApi4jPrincipalResolver(ServletContext servletContext) {
        PrincipalResolver pr = (PrincipalResolver) servletContext.getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (pr == null) {
            log.info("JsonApi4jPrincipalResolver not found in servlet context. Setting the default DefaultJsonApi4jPrincipalResolver.");
            pr = new DefaultPrincipalResolver();
            servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, pr);
        }
        return pr;
    }

}


