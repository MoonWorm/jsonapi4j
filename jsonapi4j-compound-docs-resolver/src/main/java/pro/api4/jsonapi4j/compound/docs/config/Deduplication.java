package pro.api4.jsonapi4j.compound.docs.config;

/**
 * How resource objects repeat in a compound document. JSON:API allows at most one resource object per {@code type} and
 * {@code id} pair in a compound document - {@code data} and {@code included} together - which only
 * {@link #DATA_AND_INCLUDED} guarantees.
 */
public enum Deduplication {

    /**
     * Each resource appears once across {@code data} and {@code included}: a primary resource an include path reaches is
     * not repeated in {@code included}. Spec-compliant; the default.
     */
    DATA_AND_INCLUDED,

    /**
     * Each resource appears once within {@code included}, and a primary resource an include path reaches is repeated
     * there - so a client can resolve every related resource from {@code included} alone.
     */
    INCLUDED_ONLY,

    /**
     * No deduplication: a resource is fetched and included again every time an include path reaches it.
     */
    NONE

}
