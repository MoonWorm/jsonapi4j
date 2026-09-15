package pro.api4.jsonapi4j.plugin.oas.diagnostics;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

/**
 * How this plugin reports a document it cannot vouch for.
 * <ul>
 *     <li><b>Rejections</b> throw, always: the document would be wrong rather than incomplete. Two schemas
 *     claiming one name is a rejection - one of them would be published under the other's shape, and nothing in
 *     the document would say so.</li>
 *     <li><b>Reports</b> log a warning: the document is publishable but has a loose end, such as a reference
 *     that resolves to nothing. {@code jsonapi4j.oas.diagnostics} decides what happens to them.</li>
 * </ul>
 * A document with one loose end is still more useful than no document, which is why reports warn by default.
 * Where the document is published as a contract, the flag makes that an error instead.
 */
@Slf4j
public final class OasDiagnostics {

    private OasDiagnostics() {

    }

    /**
     * Reports a document that can be published but has a loose end. The mode is passed in rather than remembered:
     * it is read from the configuration the caller was constructed with - holding it here would make it
     * process-wide, so two {@code JsonApi4j} instances in one JVM would overwrite each other's.
     *
     * @param mode how strictly this document is treated; {@link DiagnosticsMode#DISABLED} reports nothing
     */
    public static void report(DiagnosticsMode mode,
                              String message,
                              Object... arguments) {
        if (mode == null || !mode.isEnabled()) {
            return;
        }
        String formatted = String.format(message, arguments);
        if (mode.fails()) {
            throw new IllegalStateException(formatted);
        }
        log.warn(
                "{} Raise '{}.{}.{}' above {} to fail instead of warning.",
                formatted,
                JsonApi4jProperties.CONFIG_PREFIX,
                OasProperties.OAS_PROPERTY,
                OasProperties.DIAGNOSTICS_PROPERTY,
                DiagnosticsMode.WARN
        );
    }

    /**
     * Rejects a document that would say something untrue. Always throws, whatever the configuration.
     */
    public static IllegalStateException reject(String message, Object... arguments) {
        return new IllegalStateException(String.format(message, arguments));
    }

}
