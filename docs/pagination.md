---
title: "Pagination"
permalink: /pagination/
---

JsonApi4j supports [JSON:API pagination](https://jsonapi.org/format/#fetching-pagination) for all multi-resource operations — both resource reads (`GET /users`) and to-many relationship reads (`GET /users/1/relationships/citizenships`).

The framework handles the JSON:API wire format: parsing `page[...]` query parameters from the client and generating pagination links (`self`, `next`) in the response. Your code handles the actual data slicing — the framework never touches your data source.

### Pagination Strategies

JsonApi4j supports two pagination strategies: **cursor-based** and **limit-offset**. Both are exposed through the same `PaginationAwareResponse` return type — the factory method you choose determines which strategy is used.

#### Cursor-Based Pagination

Cursor-based pagination uses an opaque token (`page[cursor]`) to identify the position in the result set. This is the recommended strategy for most APIs — it handles dynamic data well and prevents skipped/duplicated items when the dataset changes between requests.

The cursor value is available via `request.getCursor()`. A `null` cursor means the client is requesting the first page.

**Server-side cursor** — use when your data source natively supports cursors (e.g., Elasticsearch scroll, DynamoDB pagination token):

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    DbPage<UserDbEntity> page = userDb.readAllUsers(request.getCursor());
    return PaginationAwareResponse.cursorAware(
        page.getEntities(),
        page.getNextCursor()  // null if this is the last page
    );
}
```

The framework generates a `next` link when the cursor is non-null.

**In-memory cursor** — use when your data source uses limit-offset internally but you want cursor-based pagination on the API. `LimitOffsetToCursorAdapter` encodes limit and offset into a Base62 cursor string:

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(request.getCursor())
        .withDefaultLimit(20);
    LimitOffsetToCursorAdapter.LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();

    List<UserDbEntity> items = userDb.readAll(limitAndOffset.getLimit(), limitAndOffset.getOffset());
    String nextCursor = adapter.nextCursor(userDb.totalCount());
    return PaginationAwareResponse.cursorAware(items, nextCursor);
}
```

For simpler cases where the full dataset is available in memory, use the convenience method that handles slicing and cursor generation internally:

```java
@Override
public PaginationAwareResponse<DownstreamCountry> readMany(JsonApiRequest request) {
    List<DownstreamCountry> allItems = countriesClient.readCountries();
    return PaginationAwareResponse.inMemoryCursorAware(
        allItems,
        request.getCursor(),
        10  // page size
    );
}
```

#### Limit-Offset Pagination

Limit-offset pagination uses `page[limit]` and `page[offset]` query parameters. This is a familiar model for SQL-backed APIs. Defaults are `limit=20` and `offset=0`.

These values are available via `request.getLimit()` and `request.getOffset()`.

**Server-side limit-offset** — use when your data source supports `LIMIT`/`OFFSET` natively and can provide a total count:

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    List<UserDbEntity> items = userDb.readAll(request.getLimit(), request.getOffset());
    long total = userDb.totalCount();
    return PaginationAwareResponse.limitOffsetAware(items, total);
}
```

The framework generates `next` links automatically based on the total count and current position.

**In-memory limit-offset** — use when slicing a full dataset in memory:

```java
return PaginationAwareResponse.inMemoryLimitOffsetAware(allItems, request.getLimit(), request.getOffset());
```

### Non-Pageable Responses

When a response should return all items without pagination (e.g., a small lookup table), use:

```java
return PaginationAwareResponse.fromItemsNotPageable(allCountries);
```

No `next` link is generated in the response.

### PaginationAwareResponse Factory Methods

| Factory Method | Strategy | Use Case |
|----------------|----------|----------|
| `cursorAware(items, nextCursor)` | Cursor | Data source provides a native cursor or token |
| `inMemoryCursorAware(items, cursor, pageSize)` | Cursor | Full dataset in memory; framework handles slicing |
| `limitOffsetAware(items, totalItems)` | Limit-offset | Data source supports LIMIT/OFFSET and total count |
| `inMemoryLimitOffsetAware(items, limit, offset)` | Limit-offset | Full dataset in memory; framework handles slicing |
| `fromItemsNotPageable(items)` | None | Return all items without pagination |
| `empty()` | None | Empty response with no items |

### Pagination Links

The framework automatically generates pagination links based on the strategy:

**Cursor-based:**
```json
"links": {
    "self": "/users?page[cursor]=DoJu",
    "next": "/users?page[cursor]=DoJw"
}
```

**Limit-offset:**
```json
"links": {
    "self": "/users?page[offset]=0&page[limit]=20",
    "next": "/users?page[offset]=20&page[limit]=20"
}
```

The `next` link is omitted when there are no more pages. You can customize link generation by overriding `resolveTopLevelLinksForMultiResourcesDoc()` on your [Resource](/domain/#resourceresource_dto) — the `PaginationContext` parameter provides the cursor, total items, and pagination mode.

### Relationship Pagination

To-many relationships have their own independent pagination. A user's `citizenships` relationship is paginated separately from the users list:

```
GET /users/1/relationships/citizenships                      → first page
GET /users/1/relationships/citizenships?page[cursor]=DoJu    → second page
```

This is handled the same way as resource pagination — your `readMany()` method returns a `PaginationAwareResponse`, and the framework generates the appropriate `next` link.

### Choosing a Strategy

| Consideration | Cursor | Limit-Offset |
|--------------|--------|--------------|
| Dynamic data (inserts/deletes between pages) | No skipped/duplicated items | Items may be skipped or duplicated |
| Jump to arbitrary page | Not supported | Supported via `page[offset]` |
| Total count required | No | Yes, for accurate `next` link |
| Implementation complexity | Lower with `LimitOffsetToCursorAdapter` | Straightforward with SQL |

The JSON:API specification does not mandate a specific pagination strategy. JsonApi4j defaults to cursor-based pagination in its examples and utilities, but both strategies are fully supported.
