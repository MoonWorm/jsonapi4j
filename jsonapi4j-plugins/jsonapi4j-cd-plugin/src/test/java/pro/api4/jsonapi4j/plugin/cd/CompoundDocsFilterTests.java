package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResult;
import pro.api4.jsonapi4j.compound.docs.IncludesChecker;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocResponseWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CompoundDocsFilterTests {

    private static final String PRIMARY_DOC = "{\"data\":{\"type\":\"users\",\"id\":\"1\"}}";
    private static final String COMPOUND_DOC = "{\"data\":{\"type\":\"users\",\"id\":\"1\"},\"included\":[{\"city\":\"Zürich\"}]}";

    private final CompoundDocsRequestSupplier requestSupplier = mock(CompoundDocsRequestSupplier.class);
    private final SelfFallbackRouting routing = mock(SelfFallbackRouting.class);
    private final ErrorsDocResponseWriter errorsDocResponseWriter = mock(ErrorsDocResponseWriter.class);
    private final IncludesChecker includesChecker = mock(IncludesChecker.class);
    private final CompoundDocsResolver resolver = mock(CompoundDocsResolver.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final ByteArrayOutputStream sent = new ByteArrayOutputStream();

    private final CompoundDocsFilter sut = new CompoundDocsFilter(
            requestSupplier, routing, errorsDocResponseWriter, includesChecker, resolver
    );

    @BeforeEach
    void setUp() throws Exception {
        when(requestSupplier.toCompoundDocsRequest(request)).thenReturn(requestWithIncludes(List.of("placeOfBirth")));
        when(response.getStatus()).thenReturn(200);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                sent.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }
        });
    }

    @Nested
    class PassThrough {

        @Test
        void doFilter_requestWithoutIncludes_passesOriginalResponseToChain() throws Exception {
            when(requestSupplier.toCompoundDocsRequest(request)).thenReturn(requestWithIncludes(null));
            FilterChain chain = mock(FilterChain.class);

            sut.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(resolver);
        }

        @Test
        void doFilter_non2xxResponse_sendsCapturedBodyUnchanged() throws Exception {
            when(response.getStatus()).thenReturn(404);

            sut.doFilter(request, response, writing("{\"errors\":[]}"));

            assertThat(sent.toString(StandardCharsets.UTF_8)).isEqualTo("{\"errors\":[]}");
            verify(resolver, never()).resolveCompoundDocs(any(), any(), any());
        }

    }

    @Nested
    class Resolution {

        @Test
        void doFilter_resolved_sendsCompoundDocumentWithItsUtf8Length() throws Exception {
            when(resolver.resolveCompoundDocs(any(), any(), any())).thenReturn(new CompoundDocsResult(COMPOUND_DOC, null));
            byte[] expected = COMPOUND_DOC.getBytes(StandardCharsets.UTF_8);

            sut.doFilter(request, response, (req, res) -> {
                res.setContentLength(PRIMARY_DOC.length());
                res.getOutputStream().write(PRIMARY_DOC.getBytes(StandardCharsets.UTF_8));
            });

            assertThat(sent.toByteArray()).isEqualTo(expected);
            verify(response).setContentLength(expected.length);
            verify(response, never()).setContentLength(PRIMARY_DOC.length());
        }

        @Test
        void doFilter_primaryWrittenAsTextWithoutFlush_resolvesTheWholeBody() throws Exception {
            when(resolver.resolveCompoundDocs(any(), any(), any())).thenReturn(new CompoundDocsResult(COMPOUND_DOC, null));

            sut.doFilter(request, response, (req, res) -> res.getWriter().write(PRIMARY_DOC));

            verify(resolver).resolveCompoundDocs(eq(PRIMARY_DOC), any(), any());
        }

        @Test
        void doFilter_resolutionFails_writesErrorForbiddingStorage() throws Exception {
            ErrorJsonApiResponseException failure = new ErrorJsonApiResponseException("boom");
            when(resolver.resolveCompoundDocs(any(), any(), any())).thenThrow(failure);

            sut.doFilter(request, response, writing(PRIMARY_DOC));

            verify(response).setHeader("Cache-Control", "no-store");
            verify(errorsDocResponseWriter).write(response, failure);
            assertThat(sent.size()).isZero();
        }

    }

    @Nested
    class UnsupportedIncludes {

        @Test
        void doFilter_unsupportedInclude_writesErrorWithoutRunningChain() throws Exception {
            UnsupportedIncludeException failure = new UnsupportedIncludeException("too deep");
            doThrow(failure).when(includesChecker).check(any());
            FilterChain chain = mock(FilterChain.class);

            sut.doFilter(request, response, chain);

            verify(errorsDocResponseWriter).write(response, failure);
            verifyNoInteractions(chain);
        }

    }

    private static FilterChain writing(String body) {
        return (req, res) -> res.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static CompoundDocsRequest requestWithIncludes(List<String> includes) {
        return new CompoundDocsRequest("GET", includes, Map.of(), Map.of(), "/users/1", Map.of());
    }

}
