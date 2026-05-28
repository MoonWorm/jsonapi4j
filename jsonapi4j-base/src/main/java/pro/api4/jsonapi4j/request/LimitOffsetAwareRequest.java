package pro.api4.jsonapi4j.request;


/**
 * Request mixin that exposes limit-offset pagination parameters
 * as defined by the JSON:API {@code page[limit]} and {@code page[offset]} query parameters.
 * <p>
 * Implement this interface when an operation supports limit-offset pagination.
 * {@link #DEFAULT_LIMIT} and {@link #DEFAULT_OFFSET} should be used when the client
 * does not supply the corresponding query parameters.
 */
public interface LimitOffsetAwareRequest {

    /** Default page size applied when the client omits {@code page[limit]}. */
    long DEFAULT_LIMIT = 20;
    /** Default offset applied when the client omits {@code page[offset]}. */
    long DEFAULT_OFFSET = 0;

    /** The JSON:API query parameter name for the page size: {@code page[limit]}. */
    String LIMIT_PARAM = "page[limit]";
    /** The JSON:API query parameter name for the page offset: {@code page[offset]}. */
    String OFFSET_PARAM = "page[offset]";

    /**
     * Returns the requested page size, or {@code null} if not provided by the client.
     *
     * @return the limit value, or {@code null}
     */
    Long getLimit();

    /**
     * Returns the requested page offset (number of items to skip), or {@code null} if not provided.
     *
     * @return the offset value, or {@code null}
     */
    Long getOffset();

}
