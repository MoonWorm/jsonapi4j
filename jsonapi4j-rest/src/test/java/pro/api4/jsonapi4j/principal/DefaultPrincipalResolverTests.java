package pro.api4.jsonapi4j.principal;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_ENTITLEMENTS_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

class DefaultPrincipalResolverTests {

    private final DefaultPrincipalResolver sut = new DefaultPrincipalResolver();

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
    }

    private void givenHeader(String name, String value) {
        when(request.getHeader(name)).thenReturn(value);
    }

    @Nested
    class UserIdResolution {

        @Test
        void resolvePrincipal_userIdHeaderPresent_principalCarriesIt() {
            givenHeader(DEFAULT_USER_ID_HEADER_NAME, "user-42");

            assertThat(sut.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        }

        @Test
        void resolvePrincipal_userIdHeaderAbsent_principalCarriesNull() {
            assertThat(sut.resolvePrincipal(request).authenticatedUserId()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t"})
        void resolvePrincipal_userIdHeaderBlank_principalCarriesNull(String blankHeader) {
            givenHeader(DEFAULT_USER_ID_HEADER_NAME, blankHeader);

            assertThat(sut.resolvePrincipal(request).authenticatedUserId()).isNull();
        }

        @Test
        void resolvePrincipal_userIdHeaderPadded_principalCarriesTrimmedValue() {
            givenHeader(DEFAULT_USER_ID_HEADER_NAME, "  user-42  ");

            assertThat(sut.resolvePrincipal(request).authenticatedUserId()).isEqualTo("user-42");
        }

    }

    @Nested
    class EntitlementsResolution {

        @Test
        void resolvePrincipal_spaceSeparatedEntitlementsHeader_principalCarriesEveryEntitlement() {
            givenHeader(DEFAULT_ENTITLEMENTS_HEADER_NAME, "ADMIN PARTNER");

            assertThat(sut.resolvePrincipal(request).authenticatedClientEntitlements()).isEqualTo(List.of("ADMIN", "PARTNER"));
        }

        @Test
        void resolvePrincipal_entitlementsHeaderAbsent_principalCarriesNull() {
            assertThat(sut.resolvePrincipal(request).authenticatedClientEntitlements()).isNull();
        }

        @Test
        void resolvePrincipal_entitlementsHeaderBlank_principalCarriesEmptyList() {
            givenHeader(DEFAULT_ENTITLEMENTS_HEADER_NAME, "   ");

            assertThat(sut.resolvePrincipal(request).authenticatedClientEntitlements()).isEmpty();
        }

    }

    @Nested
    class ScopesResolution {

        @Test
        void resolvePrincipal_spaceSeparatedScopesHeader_principalCarriesEveryScope() {
            givenHeader(DEFAULT_SCOPES_HEADER_NAME, "users.read users.write");

            assertThat(sut.resolvePrincipal(request).authenticatedClientScopes()).isEqualTo(Set.of("users.read", "users.write"));
        }

        @Test
        void resolvePrincipal_scopesHeaderAbsent_principalCarriesNull() {
            assertThat(sut.resolvePrincipal(request).authenticatedClientScopes()).isNull();
        }

        @Test
        void resolvePrincipal_scopesHeaderBlank_principalCarriesEmptySet() {
            givenHeader(DEFAULT_SCOPES_HEADER_NAME, "   ");

            assertThat(sut.resolvePrincipal(request).authenticatedClientScopes()).isEmpty();
        }

    }

}
