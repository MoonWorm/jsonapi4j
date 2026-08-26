package pro.api4.jsonapi4j.plugin.ac.config;

/**
 * How much a response says about what access control hid from the caller.
 *
 * <p>Each level discloses strictly more than the one before it, so there is no combination to reason
 * about — only how far to go. The default is {@link #NONE}: a caller learning that something was hidden
 * learns that something is there, and at {@link #FIELDS_AND_REASONS} learns which entitlement would
 * reveal it.
 */
public enum AnonymizationReportLevel {

    /** Nothing is reported. Responses look exactly as they did before the report existed. */
    NONE,

    /** A response that hid anything says so, without saying what. */
    INDICATOR,

    /** Adds the paths that were hidden, so a caller can tell an absent field from an unauthorized one. */
    FIELDS,

    /** Adds the error code of the requirement that refused each path — a description of your rules. */
    FIELDS_AND_REASONS;

    public boolean isEnabled() {
        return this != NONE;
    }

    public boolean includesFields() {
        return this == FIELDS || this == FIELDS_AND_REASONS;
    }

    public boolean includesReasons() {
        return this == FIELDS_AND_REASONS;
    }

}
