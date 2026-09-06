package pro.api4.jsonapi4j.filter.principal;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.PrincipalResolver;

import java.io.IOException;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME;

@Slf4j
public class PrincipalResolvingFilter implements Filter {

    private PrincipalResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Initializing {} ...", PrincipalResolvingFilter.class.getSimpleName());

        resolver = (PrincipalResolver) filterConfig.getServletContext().getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (resolver == null) {
            throw new UnavailableException(String.format(
                    "%s can't be initialized. %s is missing from the servlet context. "
                            + "Ensure the JsonApi4j container initializer or auto-configuration has run.",
                    PrincipalResolvingFilter.class.getSimpleName(),
                    PrincipalResolver.class.getSimpleName()
            ));
        }

        log.info("{} has been initialized", PrincipalResolvingFilter.class.getSimpleName());
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        try {
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(resolver.resolvePrincipal(servletRequest));
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            AuthenticatedPrincipalContextHolder.clear();
        }
    }

}


