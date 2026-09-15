package pro.api4.jsonapi4j.plugin.oas.diagnostics;

import lombok.extern.slf4j.Slf4j;

/**
 * How this plugin reports a document it cannot vouch for.
 * <ul>
 *     <li><b>Rejections</b> throw, always: the document would be wrong rather than incomplete. Two schemas
 *     claiming one name is a rejection - one of them would be published under the other's shape, and nothing in
 *     the document would say so.</li>
 *     <li><b>Reports</b> log a warning: the document is publishable but has a loose end, such as a reference
 *     that resolves to nothing. Turn them into rejections with {@code jsonapi4j.oas.failOnMisconfiguration}.</li>
 * </ul>
 * A document with one loose end is still more useful than no document, which is why reports warn by default.
 * Where the document is published as a contract, the flag makes that an error instead.
 */
@Slf4j
public final class OasDiagnostics {

    private OasDiagnostics() {

    }

    /**
     * Reports a document that can be published but has a loose end. The setting is passed in rather than
     * remembered: only one caller reports, and it is constructed with the configuration in hand - holding the flag
     * here would make it process-wide, so two {@code JsonApi4j} instances in one JVM would overwrite each other's.
     *
     * @param failOnMisconfiguration whether to throw instead of logging
     */
    public static void report(boolean failOnMisconfiguration,
                              String message,
                              Object... arguments) {
        String formatted = String.format(message, arguments);
        if (failOnMisconfiguration) {
            throw new IllegalStateException(formatted);
        }
        log.warn("{} Set 'jsonapi4j.oas.failOnMisconfiguration' to fail instead of warning.", formatted);
    }

    /**
     * Rejects a document that would say something untrue. Always throws, whatever the configuration.
     */
    public static IllegalStateException reject(String message, Object... arguments) {
        return new IllegalStateException(String.format(message, arguments));
    }

}
