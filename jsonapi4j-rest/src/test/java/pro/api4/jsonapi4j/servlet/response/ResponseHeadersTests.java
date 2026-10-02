package pro.api4.jsonapi4j.servlet.response;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResponseHeadersTests {

    @AfterEach
    void tearDown() {
        ResponseHeaders.clear();
    }

    @Nested
    class TakeCacheControl {

        @Test
        void takeCacheControl_propagated_returnsItAndRemovesIt() {
            ResponseHeaders.propagateCacheControl(CacheControlParser.parse("max-age=60"));

            assertThat(ResponseHeaders.takeCacheControl().getMaxAge()).isEqualTo(60L);
            assertThat(ResponseHeaders.takeCacheControl()).isNull();
        }

        @Test
        void takeCacheControl_nothingPropagated_returnsNull() {
            assertThat(ResponseHeaders.takeCacheControl()).isNull();
        }

    }

    @Nested
    class Clear {

        @Test
        void clear_collectedHeaders_discardsThem() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getStatus()).thenReturn(200);
            ResponseHeaders.propagateHeader("X-Trace", "1");
            ResponseHeaders.propagateCacheControl(CacheControlParser.parse("max-age=60"));

            ResponseHeaders.clear();
            ResponseHeaders.flush(response);

            verify(response, never()).addHeader(anyString(), anyString());
        }

    }

}
