---
title: "Filtering and Sorting"
permalink: /filtering-and-sorting/
---

JsonApi4j supports [JSON:API filtering](https://jsonapi.org/format/#fetching-filtering) and [sorting](https://jsonapi.org/format/#fetching-sorting) for multi-resource operations (`GET /users`).

The framework handles parsing query parameters from the client and making them available to your operation code via `JsonApiRequest`. Your code implements the actual data filtering and sorting logic — the framework never touches your data source.

### Filtering

JSON:API defines the `filter[...]` query parameter convention for filtering. For example:

```
GET /users?filter[region]=Europe
GET /users?filter[region]=Europe,Americas&filter[status]=active
GET /countries?filter[id]=US,NO,FI
```

#### Accessing Filters

In your operation, call `request.getFilters()` to get all filter parameters as a `Map<String, List<String>>`:

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    Map<String, List<String>> filters = request.getFilters();
    // filters = {"region" -> ["Europe", "Americas"], "status" -> ["active"]}

    return PaginationAwareResponse.cursorAware(
        userDb.readAll(filters, request.getCursor()),
        nextCursor
    );
}
```

Each filter key is the name inside `filter[...]` (e.g., `"region"` from `filter[region]`). The value is a list of strings — comma-separated values in the query parameter are split automatically.

#### The `filter[id]` Convention

The `filter[id]` parameter has special significance in JsonApi4j. The [Compound Documents](/compound-docs/) resolver uses it to batch-fetch included resources:

```
GET /countries?filter[id]=US,NO,FI
```

If your resource supports compound documents (`include` query parameter), it's strongly recommended to implement `filter[id]` support in your `readPage()` method:

```java
@Override
public PaginationAwareResponse<CountryDto> readPage(JsonApiRequest request) {
    List<String> filterIds = request.getFilters().get("id");
    if (filterIds != null && !filterIds.isEmpty()) {
        return PaginationAwareResponse.fromItemsNotPageable(
            countryService.findByIds(filterIds)
        );
    }
    return PaginationAwareResponse.cursorAware(
        countryService.findAll(request.getCursor()),
        nextCursor
    );
}
```

Without this, the compound docs resolver falls back to sequential read-by-id calls — one per included resource. See [Performance Tuning](/performance/#implement-bulk-resource-reads) for details.

#### Validation

The framework does not validate filter names or values — any `filter[...]` parameter is parsed and passed through. If you need to reject unknown filters or validate values, do so in your operation's `validateReadMultiple()` method:

```java
@Override
public void validateReadMultiple(JsonApiRequest request) {
    Set<String> allowedFilters = Set.of("id", "region", "status");
    for (String filterName : request.getFilters().keySet()) {
        if (!allowedFilters.contains(filterName)) {
            throw new ConstraintViolationException(
                DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                "Unknown filter: " + filterName,
                "filter[" + filterName + "]"
            );
        }
    }
}
```

### Sorting

JSON:API defines the `sort` query parameter for ordering results. Fields are comma-separated, with an optional `-` prefix for descending order:

```
GET /users?sort=lastName                  → ascending by lastName
GET /users?sort=-createdAt                → descending by createdAt
GET /users?sort=region,-lastName          → ascending by region, then descending by lastName
```

#### Accessing Sort Parameters

Call `request.getSortBy()` to get an ordered `Map<String, SortOrder>`:

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    Map<String, SortOrder> sortBy = request.getSortBy();
    // sortBy = {"region" -> ASC, "lastName" -> DESC}

    return PaginationAwareResponse.cursorAware(
        userDb.readAll(request.getFilters(), sortBy, request.getCursor()),
        nextCursor
    );
}
```

`SortOrder` is an enum with two values: `ASC` and `DESC`.

#### Sort Limit

The framework enforces a global cap of **5 sort fields** per request (`SortAwareRequest.NUMBER_OF_SORT_BY_GLOBAL_CAP`). Requests with more than 5 sort fields are rejected.

#### Utility Methods

`SortAwareRequest` provides static helpers for working with sort parameters:

| Method | Description |
|--------|-------------|
| `extractSortBy(sortByParam)` | Strips the `-` or `+` prefix, returning the field name |
| `extractSortOrder(sortByParam)` | Returns `DESC` for `-` prefix, `ASC` otherwise |
| `wrapWithSortOrder(sortBy, sortOrder)` | Wraps a field with `-` for DESC, plain for ASC |

These are useful when building downstream queries or forwarding sort parameters to other services.

### Combining Filters, Sorting, and Pagination

All three features work together naturally. The framework preserves filter and sort parameters in pagination links:

```
GET /users?filter[region]=Europe&sort=-createdAt&page[cursor]=DoJu
```

Response links:
```json
"links": {
    "self": "/users?filter%5Bregion%5D=Europe&sort=-createdAt&page%5Bcursor%5D=DoJu",
    "next": "/users?filter%5Bregion%5D=Europe&sort=-createdAt&page%5Bcursor%5D=DoJw"
}
```

A typical `readPage()` implementation that supports all three:

```java
@Override
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
    Map<String, List<String>> filters = request.getFilters();
    Map<String, SortOrder> sortBy = request.getSortBy();

    DbPage<UserDbEntity> page = userDb.query(filters, sortBy, request.getCursor());
    return PaginationAwareResponse.cursorAware(page.getItems(), page.getNextCursor());
}
```
