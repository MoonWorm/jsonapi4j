package pro.api4.jsonapi4j.http;

/**
 * Enumeration of HTTP header names used by the framework.
 * <p>
 * Provides a type-safe reference for the standard and custom HTTP headers that the
 * framework reads or writes during request processing and content negotiation.
 * Use {@link #getName()} to obtain the canonical header name string.
 */
public enum HttpHeaders {

    /** The standard {@code Accept} request header for content negotiation. */
    ACCEPT("Accept"),
    /** The standard {@code Content-Type} request/response header. */
    CONTENT_TYPE("Content-Type"),
    /** The standard {@code Cache-Control} request/response header. */
    CACHE_CONTROL("Cache-Control"),
    /** The standard {@code Location} response header (used on 201 Created responses). */
    LOCATION("Location"),
    /**
     * Custom header that instructs the compound-documents resolver to skip
     * {@code included} relationship expansion for the current request.
     */
    X_DISABLE_COMPOUND_DOCS("x-disable-compound-docs");

    private String name;

    HttpHeaders(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
