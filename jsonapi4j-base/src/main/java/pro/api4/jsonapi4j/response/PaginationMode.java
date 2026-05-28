package pro.api4.jsonapi4j.response;

/**
 * Identifies the pagination strategy used in a {@link PaginationAwareResponse}.
 * <p>
 * The chosen mode determines which fields of {@link PaginationContext} are populated and
 * how the framework (and doc-level resolver implementations) should interpret them.
 */
public enum PaginationMode {
    /**
     * Cursor-based (opaque) pagination.
     * {@link PaginationContext#getNextCursor()} carries the next-page cursor.
     */
    CURSOR,

    /**
     * Limit-offset pagination.
     * {@link PaginationContext#getTotalItems()} carries the total item count,
     * enabling the client to calculate page numbers and offsets.
     */
    LIMIT_OFFSET
}
