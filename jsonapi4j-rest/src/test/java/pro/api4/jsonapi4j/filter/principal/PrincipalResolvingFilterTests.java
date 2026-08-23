package pro.api4.jsonapi4j.filter.principal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

class PrincipalResolvingFilterTests {

    private final PrincipalResolvingFilter sut = new PrincipalResolvingFilter();

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws ServletException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(any())).thenReturn(new DefaultPrincipalResolver());
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        sut.init(filterConfig);
    }

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.clear();
    }

    private void givenAuthenticatedRequest() {
        when(request.getHeader(DEFAULT_USER_ID_HEADER_NAME)).thenReturn("user-42");
    }

    @Test
    void doFilter_whileTheChainRuns_principalIsAvailable() throws IOException, ServletException {
        // given
        givenAuthenticatedRequest();
        doAnswer(invocation -> {
            assertThat(AuthenticatedPrincipalContextHolder.getAuthenticatedUserId()).contains("user-42");
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        sut.doFilter(request, response, filterChain);

        // then
        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
    }

    @Test
    void doFilter_chainCompletes_principalIsCleared() throws IOException, ServletException {
        givenAuthenticatedRequest();

        sut.doFilter(request, response, filterChain);

        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
    }

    @Test
    void doFilter_chainThrows_principalIsStillCleared() throws IOException, ServletException {
        // given
        givenAuthenticatedRequest();
        doThrow(new ServletException("downstream blew up")).when(filterChain).doFilter(any(), any());

        // when
        assertThatThrownBy(() -> sut.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class);

        // then — a principal left behind would leak onto the next request served by this pooled thread
        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
    }

    @Test
    void doFilter_anonymousRequest_leavesNoPrincipalBehind() throws IOException, ServletException {
        sut.doFilter(request, response, filterChain);

        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
    }

}
