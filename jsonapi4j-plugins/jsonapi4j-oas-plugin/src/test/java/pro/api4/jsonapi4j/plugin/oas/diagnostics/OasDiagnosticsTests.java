package pro.api4.jsonapi4j.plugin.oas.diagnostics;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The mode decides what happens to a document that is publishable but has a loose end. It deliberately does not
 * decide what happens to one that would say something untrue - a rejection throws whatever the mode, because no
 * setting should let a schema published under another's shape reach a client.
 */
class OasDiagnosticsTests {

    @Nested
    class Reports {

        @Test
        void report_disabled_staysSilent() {
            assertThatCode(() -> OasDiagnostics.report(DiagnosticsMode.DISABLED, "a loose end in '%s'", "x"))
                    .doesNotThrowAnyException();
        }

        @Test
        void report_warn_doesNotInterruptGeneration() {
            assertThatCode(() -> OasDiagnostics.report(DiagnosticsMode.WARN, "a loose end in '%s'", "x"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = DiagnosticsMode.class, names = {"FAIL_ON_REQUEST", "FAIL_ON_STARTUP"})
        void report_failingMode_throwsWithTheFormattedMessage(DiagnosticsMode diagnostics) {
            assertThatThrownBy(() -> OasDiagnostics.report(diagnostics, "a loose end in '%s'", "x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("a loose end in 'x'");
        }

        /**
         * A mode that never reached the configuration is not a reason to fail a document the framework could
         * otherwise publish.
         */
        @Test
        void report_noMode_staysSilent() {
            assertThatCode(() -> OasDiagnostics.report(null, "a loose end in '%s'", "x"))
                    .doesNotThrowAnyException();
        }

    }

    @Nested
    class Rejections {

        @Test
        void reject_always_buildsAThrowableWithTheFormattedMessage() {
            assertThat(OasDiagnostics.reject("two schemas claim '%s'", "Address"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("two schemas claim 'Address'");
        }

    }

    @Nested
    class Modes {

        @Test
        void diagnostics_notConfigured_warns() {
            assertThat(new DefaultOasProperties().diagnostics()).isEqualTo(DiagnosticsMode.WARN);
        }

        @ParameterizedTest
        @EnumSource(value = DiagnosticsMode.class, names = {"FAIL_ON_REQUEST", "FAIL_ON_STARTUP"})
        void fails_failingModes_isTrue(DiagnosticsMode diagnostics) {
            assertThat(diagnostics.fails()).isTrue();
            assertThat(diagnostics.isEnabled()).isTrue();
        }

        @Test
        void generatesAtStartup_onlyFailOnStartup_isTrue() {
            assertThat(DiagnosticsMode.FAIL_ON_STARTUP.generatesAtStartup()).isTrue();
            assertThat(DiagnosticsMode.FAIL_ON_REQUEST.generatesAtStartup()).isFalse();
            assertThat(DiagnosticsMode.WARN.generatesAtStartup()).isFalse();
            assertThat(DiagnosticsMode.DISABLED.generatesAtStartup()).isFalse();
        }

        @Test
        void isEnabled_disabled_isFalse() {
            assertThat(DiagnosticsMode.DISABLED.isEnabled()).isFalse();
            assertThat(DiagnosticsMode.DISABLED.fails()).isFalse();
        }

    }

}
