package pro.api4.jsonapi4j.compatibility;

public enum JsonApi4jCompatibilityMode {
    STRICT,
    LEGACY;

    public static JsonApi4jCompatibilityMode fromLegacyMode(boolean legacyMode) {
        return legacyMode ? LEGACY : STRICT;
    }

    public boolean isLegacyMode() {
        return this == LEGACY;
    }
}
