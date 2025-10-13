package io.jsonapi4j.servlet.request.body;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * This filter helps to cache the request body. By default, once the body has been read - it's not possible to read it again.
 */
public class RequestBodyCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            RequestBodyCachingRequestWrapper wrappedRequest = new RequestBodyCachingRequestWrapper(
                    (HttpServletRequest) request
            );
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

}