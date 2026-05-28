package pro.api4.jsonapi4j.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable snapshot of pagination state returned alongside a page of items.
 * <p>
 * Built by the framework from the {@link PaginationAwareResponse} that an operation's data
 * supplier returns, and then passed to {@link pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver}
 * and {@link pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver} so that
 * implementations can embed pagination links (e.g. {@code "next"}) or total-count meta in the response.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class PaginationContext {

    /** The pagination strategy in use for this response. */
    private PaginationMode mode;

    /**
     * The opaque cursor pointing to the next page, or {@code null} if there is no next page.
     * Populated only when {@link #mode} is {@link PaginationMode#CURSOR}.
     */
    private String nextCursor;

    /**
     * The total number of items across all pages, or {@code null} if unknown.
     * Populated by {@link PaginationMode#LIMIT_OFFSET} responses.
     */
    private Long totalItems;

}
