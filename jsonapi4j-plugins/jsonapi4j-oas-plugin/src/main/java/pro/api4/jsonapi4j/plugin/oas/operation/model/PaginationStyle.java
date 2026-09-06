package pro.api4.jsonapi4j.plugin.oas.operation.model;

/**
 * The pagination an operation honours.
 * <p>
 * Every paginated request carries both styles — {@code JsonApiRequest} exposes cursor and limit/offset alike — but
 * which one an operation acts on is a property of its implementation that nothing else can observe. Declaring it
 * keeps the document from advertising a parameter the operation would silently ignore.
 */
public enum PaginationStyle {

    /**
     * {@code page[cursor]}: the server hands out an opaque pointer to the next page.
     */
    CURSOR,

    /**
     * {@code page[limit]} and {@code page[offset]}: the client asks for a window by position.
     */
    LIMIT_OFFSET

}
