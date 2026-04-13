package pro.api4.jsonapi4j.servlet.response.cache;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.servlet.response.ResponseHeaders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResponseHeadersTests {

    @Mock
    private HttpServletResponse response;

    @AfterEach
    void cleanup() {
        // ensure ThreadLocal is always cleaned up between tests
        ResponseHeaders.flush(response);
    }

    // --- propagateCacheControl(HttpServletResponse) ---

    @Test
    void propagateCacheControl_fromResponse_extractsAndPropagates() {
        // given
        HttpServletResponse downstreamResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(downstreamResponse.getHeader(HttpHeaders.CACHE_CONTROL.getName())).thenReturn("max-age=300");
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateCacheControl(downstreamResponse);
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader(HttpHeaders.CACHE_CONTROL.getName(), "max-age=300");
    }

    @Test
    void propagateCacheControl_fromNullResponse_doesNothing() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateCacheControl((HttpServletResponse) null);
        ResponseHeaders.flush(response);

        // then
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
    }

    @Test
    void propagateCacheControl_fromResponseWithBlankHeader_doesNothing() {
        // given
        HttpServletResponse downstreamResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(downstreamResponse.getHeader(HttpHeaders.CACHE_CONTROL.getName())).thenReturn("  ");
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateCacheControl(downstreamResponse);
        ResponseHeaders.flush(response);

        // then
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
    }

    // --- propagateCacheControl(CacheControlDirectives) ---

    @Test
    void propagateCacheControl_fromDirectives_propagatesFormattedValue() {
        // given
        CacheControlDirectives directives = CacheControlDirectives.ofMaxAge(600);
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateCacheControl(directives);
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader(HttpHeaders.CACHE_CONTROL.getName(), "max-age=600");
    }

    @Test
    void propagateCacheControl_fromDirectives_replacedBySubsequentCall() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when - second call replaces the first
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(300));
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(60));
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader(HttpHeaders.CACHE_CONTROL.getName(), "max-age=60");
        verify(response, never()).addHeader(HttpHeaders.CACHE_CONTROL.getName(), "max-age=300");
    }

    // --- flush: Cache-Control special behavior ---

    @Test
    void flush_cacheControl_notPropagatedForNon2xxStatus() {
        // given
        when(response.getStatus()).thenReturn(500);

        // when
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(300));
        ResponseHeaders.flush(response);

        // then
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
    }

    @Test
    void flush_cacheControl_notPropagatedFor4xxStatus() {
        // given
        when(response.getStatus()).thenReturn(404);

        // when
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(300));
        ResponseHeaders.flush(response);

        // then
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
    }

    @Test
    void flush_cacheControl_notOverriddenIfAlreadySet() {
        // given
        when(response.getStatus()).thenReturn(200);
        when(response.getHeader(HttpHeaders.CACHE_CONTROL.getName())).thenReturn("no-cache");

        // when
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(300));
        ResponseHeaders.flush(response);

        // then
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
    }

    // --- propagateHeader ---

    @Test
    void propagateHeader_singleValue_addedToResponse() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateHeader("X-Custom", "value1");
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader("X-Custom", "value1");
    }

    @Test
    void propagateHeader_multipleValues_allAddedToResponse() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateHeader("X-Custom", "value1");
        ResponseHeaders.propagateHeader("X-Custom", "value2");
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader("X-Custom", "value1");
        verify(response).addHeader("X-Custom", "value2");
    }

    @Test
    void propagateHeader_multipleHeaders_allPropagated() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateHeader("X-First", "a");
        ResponseHeaders.propagateHeader("X-Second", "b");
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader("X-First", "a");
        verify(response).addHeader("X-Second", "b");
    }

    @Test
    void propagateHeader_customHeadersPropagatedEvenForNon2xxStatus() {
        // given
        when(response.getStatus()).thenReturn(500);

        // when
        ResponseHeaders.propagateHeader("X-Custom", "value");
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader("X-Custom", "value");
    }

    // --- flush: cleanup ---

    @Test
    void flush_clearsThreadLocal_subsequentFlushDoesNothing() {
        // given
        when(response.getStatus()).thenReturn(200);
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(300));
        ResponseHeaders.propagateHeader("X-Custom", "value");

        // when - first flush applies headers
        ResponseHeaders.flush(response);

        // then - second flush on a fresh mock does nothing
        HttpServletResponse secondResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(secondResponse.getStatus()).thenReturn(200);
        ResponseHeaders.flush(secondResponse);

        verify(secondResponse, never()).addHeader(
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString()
        );
    }

    // --- mixed: Cache-Control + custom headers ---

    @Test
    void flush_cacheControlAndCustomHeaders_bothPropagatedFor2xx() {
        // given
        when(response.getStatus()).thenReturn(200);

        // when
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(120));
        ResponseHeaders.propagateHeader("X-Request-Id", "abc-123");
        ResponseHeaders.flush(response);

        // then
        verify(response).addHeader(HttpHeaders.CACHE_CONTROL.getName(), "max-age=120");
        verify(response).addHeader("X-Request-Id", "abc-123");
    }

    @Test
    void flush_cacheControlSkippedButCustomHeadersPropagated_forNon2xx() {
        // given
        when(response.getStatus()).thenReturn(403);

        // when
        ResponseHeaders.propagateCacheControl(CacheControlDirectives.ofMaxAge(120));
        ResponseHeaders.propagateHeader("X-Request-Id", "abc-123");
        ResponseHeaders.flush(response);

        // then - Cache-Control not propagated for non-2xx
        verify(response, never()).addHeader(
                org.mockito.Mockito.eq(HttpHeaders.CACHE_CONTROL.getName()),
                org.mockito.Mockito.anyString()
        );
        // custom header still propagated
        verify(response).addHeader("X-Request-Id", "abc-123");
    }

}
