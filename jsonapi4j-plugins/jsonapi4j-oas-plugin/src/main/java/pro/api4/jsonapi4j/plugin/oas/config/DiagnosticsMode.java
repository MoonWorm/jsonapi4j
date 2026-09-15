package pro.api4.jsonapi4j.plugin.oas.config;

/**
 * How strictly a generated document that cannot be vouched for is treated, in increasing order of strictness.
 * <p>
 * This governs <b>reports</b> - a document that is publishable but has a loose end, such as a {@code $ref} resolving
 * to nothing or a scope requirement no configured grant flow can carry. It does not govern <b>rejections</b>, which
 * always throw whatever the mode: two schemas claiming one name would publish one of them under the other's shape,
 * and no setting should let that reach a client.
 * <p>
 * {@link #FAIL_ON_STARTUP} is the only mode that builds the document eagerly, which is what makes it the only one
 * where a rejection cannot first surface in production - at the cost of the generation work at every boot.
 */
public enum DiagnosticsMode {

    /**
     * Report nothing, and skip the checks that exist only to report - the document is not walked looking for
     * unresolved references. Rejections still throw.
     */
    DISABLED,

    /**
     * Log a warning and publish the document anyway. The default: one loose end still leaves a document more useful
     * than no document, and the warning names what is wrong.
     */
    WARN,

    /**
     * Refuse to serve a document with a loose end. Generation stays lazy, so boot is unaffected and the failure
     * surfaces on a request for the document. For an application that cannot afford generation at startup - a
     * native image, say - but should never publish a document it cannot vouch for.
     */
    FAIL_ON_REQUEST,

    /**
     * Build the document during startup and refuse to start if it has a loose end. For a document published as a
     * contract: it is the only mode under which a broken document cannot reach a running application.
     */
    FAIL_ON_STARTUP;

    /**
     * @return whether anything is reported at all - {@code false} only for {@link #DISABLED}
     */
    public boolean isEnabled() {
        return this != DISABLED;
    }

    /**
     * @return whether a report throws instead of logging
     */
    public boolean fails() {
        return this == FAIL_ON_REQUEST || this == FAIL_ON_STARTUP;
    }

    /**
     * @return whether the document is generated during startup rather than on the first request for it
     */
    public boolean generatesAtStartup() {
        return this == FAIL_ON_STARTUP;
    }

}
