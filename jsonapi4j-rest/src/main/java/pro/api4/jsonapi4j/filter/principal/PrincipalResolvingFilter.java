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

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME;

@Slf4j
public class PrincipalResolvingFilter implements Filter {

    private PrincipalResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Initializing {} ...", PrincipalResolvingFilter.class.getSimpleName());
        resolver = initJsonApi4jPrincipalResolver(filterConfig.getServletContext());
        log.info("{} has been initialized", PrincipalResolvingFilter.class.getSimpleName());
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
            log.info(
                    "{} not found in servlet context. Setting the default implementation {}.",
                    PrincipalResolver.class.getSimpleName(),
                    DefaultPrincipalResolver.class.getSimpleName()
            );
            pr = new DefaultPrincipalResolver();
            servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, pr);
        } else {
            log.info(
                    "{} has been found in the Servlet Context under {} attribute. Applying it.",
                    PrincipalResolver.class.getSimpleName(),
                    PRINCIPAL_RESOLVER_ATT_NAME
            );
        }
        return pr;
    }

}


