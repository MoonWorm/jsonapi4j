package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BufferedResponseWrapperTests {

    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final BufferedResponseWrapper sut = new BufferedResponseWrapper(response);

    @Nested
    class Capture {

        @Test
        void getCapturedBody_writerNotFlushed_includesPendingText() {
            sut.getWriter().write("{\"data\":null}");

            assertThat(new String(sut.getCapturedBody(), StandardCharsets.UTF_8)).isEqualTo("{\"data\":null}");
        }

        @Test
        void getCapturedBodyAsString_nonAsciiWrittenAsBytes_decodesUtf8() throws Exception {
            sut.getOutputStream().write("{\"city\":\"Zürich\"}".getBytes(StandardCharsets.UTF_8));

            assertThat(sut.getCapturedBodyAsString()).isEqualTo("{\"city\":\"Zürich\"}");
        }

        @Test
        void getCapturedBody_nonAsciiWrittenAsText_encodesUtf8() {
            sut.getWriter().write("Zürich");

            assertThat(sut.getCapturedBody()).isEqualTo("Zürich".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        void getCapturedBody_nothingWritten_isEmpty() {
            assertThat(sut.getCapturedBody()).isEmpty();
        }

        @Test
        void resetBuffer_afterWrite_discardsCapturedBody() {
            sut.getWriter().write("partial");

            sut.resetBuffer();

            assertThat(sut.getCapturedBody()).isEmpty();
        }

    }

    @Nested
    class WrappedResponse {

        @Test
        void flushBuffer_called_doesNotCommitWrappedResponse() throws Exception {
            sut.flushBuffer();

            verify(response, never()).flushBuffer();
        }

        @Test
        void setContentLength_called_isNotPassedOn() {
            sut.setContentLength(10);
            sut.setContentLengthLong(10L);

            verify(response, never()).setContentLength(anyInt());
            verify(response, never()).setContentLengthLong(anyLong());
        }

        @Test
        void setHeader_contentLengthInAnyCase_isNotPassedOn() {
            sut.setHeader("content-length", "10");
            sut.addHeader("Content-Length", "10");
            sut.setIntHeader("CONTENT-LENGTH", 10);
            sut.addIntHeader("Content-Length", 10);

            verify(response, never()).setHeader(anyString(), anyString());
            verify(response, never()).addHeader(anyString(), anyString());
            verify(response, never()).setIntHeader(anyString(), anyInt());
            verify(response, never()).addIntHeader(anyString(), anyInt());
        }

        @Test
        void setHeader_otherHeader_isPassedOn() {
            sut.setHeader("Cache-Control", "max-age=60");

            verify(response).setHeader("Cache-Control", "max-age=60");
        }

    }

}
