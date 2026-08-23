package pro.api4.jsonapi4j.principal;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtPrincipalResolverTests {

    private static final String ENTITLEMENTS_CLAIM = "entitlements";

    private final JwtPrincipalResolver resolver = new JwtPrincipalResolver();

    private HttpServletRequest request;
    private Map<String, Object> requestAttributes;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        requestAttributes = new HashMap<>();
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(any(), any());
        when(request.getAttribute(any())).thenAnswer(invocation -> requestAttributes.get(invocation.getArgument(0)));
    }

    private static String jwtWithPayload(String payloadJson) {
        String header = encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encode(payloadJson);
        return String.format("%s.%s.%s", header, payload, "not-a-verified-signature");
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private void givenAuthorizationHeader(String headerValue) {
        when(request.getHeader("Authorization")).thenReturn(headerValue);
    }

    // --- happy path ---

    @Test
    void resolvesUserIdAndScopesFromBearerToken() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload(
                "{\"sub\":\"user-42\",\"scope\":\"read write\"}"));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void resolvesAllClaimsAsAttributes() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload(
                "{\"sub\":\"user-42\",\"email\":\"user@api4.pro\",\"exp\":1893456000}"));

        assertThat(resolver.resolvePrincipal(request).attributes())
                .containsEntry("sub", "user-42")
                .containsEntry("email", "user@api4.pro")
                .containsEntry("exp", 1893456000);
    }

    @Test
    void resolvesNestedClaimsAsAttributes() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload(
                "{\"sub\":\"user-42\",\"realm_access\":{\"roles\":[\"admin\"]}}"));

        assertThat(resolver.resolvePrincipal(request).attributes()).containsKey("realm_access");
    }

    @Test
    void acceptsTokenWithoutBase64Padding() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload("{\"sub\":\"a\"}"));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("a");
    }

    @Test
    void isCaseInsensitiveAboutTheBearerPrefix() {
        givenAuthorizationHeader("bearer " + jwtWithPayload("{\"sub\":\"user-42\"}"));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
    }

    @Test
    void resolvesEntitlementsFromConfiguredClaim() {
        JwtPrincipalResolver entitlementsResolver =
                JwtPrincipalResolver.withEntitlementsClaim(ENTITLEMENTS_CLAIM);
        givenAuthorizationHeader("Bearer " + jwtWithPayload(
                "{\"sub\":\"user-42\",\"entitlements\":\"ADMIN\"}"));

        List<String> entitlements = entitlementsResolver.resolvePrincipal(request).authenticatedClientEntitlements();
        assertThat(entitlements).isNotNull().hasSize(1);
        assertThat(entitlements.getFirst()).isNotNull().isEqualTo("ADMIN");
    }

    @Test
    void readsTokenFromCustomHeader() {
        JwtPrincipalResolver customResolver = new JwtPrincipalResolver(
                "X-Forwarded-Access-Token", new ClaimsPrincipalMapper(), new com.fasterxml.jackson.databind.ObjectMapper());
        when(request.getHeader("X-Forwarded-Access-Token"))
                .thenReturn("Bearer " + jwtWithPayload("{\"sub\":\"gateway-user\"}"));

        assertThat(customResolver.resolvePrincipal(request).authenticatedUserId()).isEqualTo("gateway-user");
    }

    @Test
    void resolvePrincipal_oneCall_readsTheTokenOnce() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload(
                "{\"sub\":\"user-42\",\"scope\":\"read\"}"));

        resolver.resolvePrincipal(request);

        verify(request, times(1)).getHeader("Authorization");
    }

    @Test
    void resolvePrincipal_neverWritesToTheRequest() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload("{\"sub\":\"user-42\"}"));

        resolver.resolvePrincipal(request);

        verify(request, never()).setAttribute(any(), any());
    }

    // --- absent or malformed tokens ---

    @Test
    void resolvesNothingWhenAuthorizationHeaderIsAbsent() {
        givenAuthorizationHeader(null);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientEntitlements()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "Basic dXNlcjpwYXNz",
            "Bearer ",
            "Bearer not-a-jwt",
            "Bearer only.two",
            "Bearer a.b.c.d",
            "Bearer header.!!!not-base64!!!.signature"
    })
    void resolvesNothingForUnusableAuthorizationHeaders(String headerValue) {
        givenAuthorizationHeader(headerValue);

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

    @Test
    void resolvesNothingWhenPayloadIsNotAJsonObject() {
        givenAuthorizationHeader("Bearer " + jwtWithPayload("\"just-a-string\""));

        assertThat(resolver.resolvePrincipal(request).authenticatedUserId()).isNull();
        assertThat(resolver.resolvePrincipal(request).attributes()).isEmpty();
    }

}
