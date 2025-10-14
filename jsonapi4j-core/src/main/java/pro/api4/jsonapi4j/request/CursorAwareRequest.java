package pro.api4.jsonapi4j.request;


import pro.api4.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter;

public interface CursorAwareRequest {

    String CURSOR_PARAM = "page[cursor]";

    /**
     * Relevant for all multi resource or multi resource linkages operations, e.g.
     * <code>GET /countries/NO/currencies?page[cursor]=Jfsjdhfks</code> where 'currencies' is OneToMany relationship.
     * For the example above this method will return <code>Jfsjdhfks</code>
     * <p>
     * The framework by default encourages cursor-based pagination policy. But other strategies can also be used if
     * needed. Refer JSON:API specification for more details:
     * <a href="https://jsonapi.org/format/#fetching-pagination">pagination</a>
     * <p>
     * For all such operations will hold a value of the cursor. Can be <code>null</code> even for operations that
     * supports cursor - that would mean that the client requests the very first page of resources.
     * <p>
     * {@link LimitOffsetToCursorAdapter} can used to adjusted downstream pagination
     * that is 'limit-offset' by its nature and get a cursor by transforming 'limit' and 'offset' into base-65-ed string.
     *
     * @return cursor value or <code>null</code>
     */
    String getCursor();

}
