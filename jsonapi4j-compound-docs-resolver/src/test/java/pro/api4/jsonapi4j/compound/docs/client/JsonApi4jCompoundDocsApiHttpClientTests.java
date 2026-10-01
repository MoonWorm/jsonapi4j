package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApi4jCompoundDocsApiHttpClientTests {

    private static final CompoundDocsResolverConfig CONFIG = new CompoundDocsResolverConfig(
            true, 2, UnsupportedIncludeStrategy.FAIL, 100, ErrorStrategy.IGNORE, List.of(Propagation.HEADERS), Set.of("Authorization", "Cookie"), Deduplication.DATA_AND_INCLUDED, 1000, 300, false, 1
    );
    private static final CompoundDocsRequest REQUEST = new CompoundDocsRequest(
            "GET", List.of("placeOfBirth"), Map.of(), Map.of(), "/users/1", Map.of()
    );

    private static final String UNSUPPORTED_CURRENCIES =
            "{\"errors\":[{\"code\":\"UNSUPPORTED_INCLUDE\",\"meta\":{\"path\":\"currencies\"}}]}";

    private final JsonApi4jCompoundDocsApiHttpClient sut = new JsonApi4jCompoundDocsApiHttpClient(new ObjectMapper(), CONFIG);
    private HttpServer server;

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Nested
    class DoBatchFetch {

        @Test
        public void doBatchFetch_successfulResponse_returnsResourcesAndDisablesCompoundDocsDownstream() throws IOException {
            AtomicReference<Headers> requestHeaders = new AtomicReference<>();
            startServer(200, "{\"data\":[{\"type\":\"countries\",\"id\":\"US\"}]}", 0, requestHeaders);

            HttpFetchResult result = sut.doBatchFetch(batch(), REQUEST);

            assertThat(result.resources()).extracting(r -> r.idAndType().getId()).containsExactly("US");
            assertThat(result.failed()).isFalse();
            assertThat(result.directives().getMaxAge()).isEqualTo(300L);
            assertThat(requestHeaders.get().getFirst("X-Disable-Compound-Docs")).isEqualTo("true");
        }

        @Test
        public void doBatchFetch_non200Response_throwsErrorJsonApiResponseException() throws IOException {
            startServer(503, "unavailable", 0, new AtomicReference<>());

            assertThatThrownBy(() -> sut.doBatchFetch(batch(), REQUEST))
                    .isExactlyInstanceOf(ErrorJsonApiResponseException.class)
                    .hasMessageContaining("503");
        }

        @Test
        public void doBatchFetch_badRequestRejectingRequestedIncludes_throwsRejectedIncludesException() throws IOException {
            startServer(400, UNSUPPORTED_CURRENCIES, 0, new AtomicReference<>());

            BatchFetch withIncludes = new BatchFetch(
                    batch().domainSettings(), "countries", Set.of("US"), Set.of("currencies", "economy")
            );

            assertThatThrownBy(() -> sut.doBatchFetch(withIncludes, REQUEST))
                    .isInstanceOfSatisfying(RejectedIncludesException.class, e -> {
                        assertThat(e.getResourceType()).isEqualTo("countries");
                        assertThat(e.getRelationshipNames()).containsExactly("currencies");
                    });
        }

        @Test
        public void doBatchFetch_badRequestRejectingIncludesNotRequested_throwsErrorJsonApiResponseException() throws IOException {
            startServer(400, UNSUPPORTED_CURRENCIES, 0, new AtomicReference<>());

            assertThatThrownBy(() -> sut.doBatchFetch(batch(), REQUEST))
                    .isExactlyInstanceOf(ErrorJsonApiResponseException.class)
                    .hasMessageContaining("400");
        }

        @Test
        public void doBatchFetch_responseSlowerThanTotalTimeout_throwsDownstreamTimeoutException() throws IOException {
            startServer(200, "{\"data\":[]}", 2000, new AtomicReference<>());

            assertThatThrownBy(() -> sut.doBatchFetch(batch(), REQUEST))
                    .isInstanceOf(DownstreamTimeoutException.class);
        }

        @Test
        public void doBatchFetch_bodyIsNotJson_throwsErrorJsonApiResponseException() throws IOException {
            startServer(200, "<html>oops</html>", 0, new AtomicReference<>());

            assertThatThrownBy(() -> sut.doBatchFetch(batch(), REQUEST))
                    .isExactlyInstanceOf(ErrorJsonApiResponseException.class);
        }

        @Test
        public void doBatchFetch_connectionRefused_throwsErrorJsonApiResponseException() {
            BatchFetch unreachable = new BatchFetch(
                    DomainSettings.of(URI.create("http://127.0.0.1:1/jsonapi")), "countries", Set.of("US"), Set.of()
            );

            assertThatThrownBy(() -> sut.doBatchFetch(unreachable, REQUEST))
                    .isExactlyInstanceOf(ErrorJsonApiResponseException.class);
        }

    }

    private void startServer(int status,
                             String body,
                             long delayMs,
                             AtomicReference<Headers> requestHeaders) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jsonapi/countries", exchange -> {
            requestHeaders.set(exchange.getRequestHeaders());
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Cache-Control", "max-age=300");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    private BatchFetch batch() {
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jsonapi");
        return new BatchFetch(DomainSettings.of(baseUrl), "countries", Set.of("US"), Set.of());
    }

    @Nested
    class HeaderPropagation {

        @Test
        public void doBatchFetch_headersShapingTheResponse_areReplacedByJsonApiDefaults() throws IOException {
            AtomicReference<Headers> requestHeaders = new AtomicReference<>();
            startServer(200, "{\"data\":[]}", 0, requestHeaders);

            sut.doBatchFetch(batch(), requestWithHeaders(Map.of(
                    "Accept", List.of("text/html"),
                    "Accept-Encoding", List.of("gzip, br"),
                    "If-None-Match", List.of("\"v1\""),
                    "Range", List.of("bytes=0-10"),
                    "Accept-Language", List.of("de")
            )));

            Headers received = requestHeaders.get();
            assertThat(received.get("Accept")).containsExactly("application/vnd.api+json");
            assertThat(received).doesNotContainKeys("Accept-encoding", "If-none-match", "Range");
            assertThat(received.getFirst("Accept-Language")).isEqualTo("de");
        }

        @Test
        public void doBatchFetch_domainNotTrustedWithCredentials_leavesThemOut() throws IOException {
            AtomicReference<Headers> requestHeaders = new AtomicReference<>();
            startServer(200, "{\"data\":[]}", 0, requestHeaders);

            sut.doBatchFetch(batch(), requestWithHeaders(Map.of(
                    "authorization", List.of("Bearer token"),
                    "Cookie", List.of("session=1"),
                    "X-Tenant", List.of("acme")
            )));

            assertThat(requestHeaders.get()).doesNotContainKeys("Authorization", "Cookie");
            assertThat(requestHeaders.get().getFirst("X-Tenant")).isEqualTo("acme");
        }

        @Test
        public void doBatchFetch_domainTrustedWithCredentials_sendsThemWithCookiesJoined() throws IOException {
            AtomicReference<Headers> requestHeaders = new AtomicReference<>();
            startServer(200, "{\"data\":[]}", 0, requestHeaders);
            BatchFetch trusted = new BatchFetch(
                    new DomainSettings(batch().domainSettings().url(), 20, true), "countries", Set.of("US"), Set.of()
            );

            sut.doBatchFetch(trusted, requestWithHeaders(Map.of(
                    "Authorization", List.of("Bearer token"),
                    "Cookie", List.of("session=1", "theme=dark")
            )));

            assertThat(requestHeaders.get().getFirst("Authorization")).isEqualTo("Bearer token");
            assertThat(requestHeaders.get().get("Cookie")).containsExactly("session=1; theme=dark");
        }

        @Test
        public void doBatchFetch_headerWithSeveralValues_sendsEachOfThem() throws IOException {
            AtomicReference<Headers> requestHeaders = new AtomicReference<>();
            startServer(200, "{\"data\":[]}", 0, requestHeaders);

            sut.doBatchFetch(batch(), requestWithHeaders(Map.of("X-Feature", List.of("a", "b"))));

            assertThat(requestHeaders.get().get("X-Feature")).containsExactly("a", "b");
        }

        private CompoundDocsRequest requestWithHeaders(Map<String, List<String>> headers) {
            return new CompoundDocsRequest("GET", List.of("placeOfBirth"), Map.of(), headers, "/users/1", Map.of());
        }

    }

    @Nested
    class IsPropagatable {

        @ParameterizedTest
        @ValueSource(strings = {
                "Accept", "Accept-Encoding", "Content-Type", "If-Match", "If-Modified-Since", "If-None-Match",
                "If-Range", "If-Unmodified-Since", "Range", "X-Disable-Compound-Docs"
        })
        public void isPropagatable_headerShapingTheResponse_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Keep-Alive", "Proxy-Connection", "TE", "Trailer", "Transfer-Encoding"})
        public void isPropagatable_hopByHopHeader_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }


        @ParameterizedTest
        @ValueSource(strings = {"Forwarded", "X-Forwarded-For", "x-forwarded-for", "X-Real-IP"})
        public void isPropagatable_clientAddressHeader_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Host", "Connection", "Content-Length", "Expect", "Upgrade"})
        public void isPropagatable_restrictedHeader_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Authorization",
                "Accept-Language",
                "traceparent",
                "X-Forwarded-Proto",
                "X-Forwarded-Host",
                "X-Forwarded-User",
                "X-Forwarded-Client-Cert"
        })
        public void isPropagatable_regularHeader_returnsTrue(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isTrue();
        }

    }

}
