package pro.api4.jsonapi4j.plugin.ac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.principal.tier.TierAdmin;
import pro.api4.jsonapi4j.principal.tier.TierPublic;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the condition behind the "authenticated principal carries no access tier" warning. The warning is
 * a log line; what matters — and what is pinned here — is that it fires only for a principal that
 * authenticated successfully yet has no tier, and stays silent for anonymous callers and for ordinary
 * denials where the principal simply holds a lower tier.
 */
class MissingAccessTierDiagnosticTests {

    private static final String USER_ID = "user-42";

    private final DefaultAccessControlEvaluator evaluator =
            new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry());

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(null);
    }

    @Test
    void flagsAnAuthenticatedPrincipalThatCarriesNoAccessTier() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(null, Set.of("users.read"), USER_ID, Map.of()));

        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isTrue();
    }

    @Test
    void ignoresAnonymousCallersWhichLegitimatelyHaveNoTier() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(null, Set.of(), null, Map.of()));

        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isFalse();
    }

    @Test
    void ignoresABlankUserIdWhichDoesNotCountAsAuthenticated() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(null, Set.of(), "   ", Map.of()));

        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isFalse();
    }

    @Test
    void ignoresAnOrdinaryDenialWhereThePrincipalHoldsALowerTier() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(new TierPublic(), Set.of(), USER_ID, Map.of()));

        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isFalse();
    }

    @Test
    void ignoresAPrincipalHoldingASufficientTier() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(new TierAdmin(), Set.of(), USER_ID, Map.of()));

        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isFalse();
    }

    @Test
    void ignoresAnAbsentPrincipalAltogether() {
        assertThat(evaluator.isMissingAccessTierMisconfiguration()).isFalse();
    }

}
