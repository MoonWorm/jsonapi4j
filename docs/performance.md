---
title: "Performance Tuning"
permalink: /performance/
---

JsonApi4j's performance is dominated by [relationship resolution](/request-processing-pipeline/#6-fetch-relationship-data-parallel) — the number of downstream calls made when building responses, especially for [compound documents](/compound-docs/). All optimizations below target reducing or parallelizing these calls.

## Implement Bulk Resource Reads

When compound documents are enabled, the resolver fetches included resources by their IDs. If your `ReadMultipleResourcesOperation` supports the `filter[id]` parameter (see [Filtering and Sorting](/filtering-and-sorting/#the-filterid-convention)), the resolver batches these into a single request:

```
GET /countries?filter[id]=US,DE,FR
```

If no bulk operation is available, the framework falls back to sequential read-by-id calls — one HTTP request per resource. For 20 included resources, that is 20 sequential calls instead of 1.

```java
public class CountryOperations implements ResourceOperations<CountryDto> {

    @Override
    public PaginationAwareResponse<CountryDto> readPage(JsonApiRequest request) {
        List<String> filterIds = request.getFilterIds(); // e.g. ["US", "DE", "FR"]
        if (filterIds != null && !filterIds.isEmpty()) {
            return PaginationAwareResponse.fromItemsNotPageable(
                countryService.findByIds(filterIds)
            );
        }
        return PaginationAwareResponse.cursorAware(
            countryService.findAll(request.getPaginationRequest()),
            request.getCursor()
        );
    }
}
```

This is the single most impactful optimization for compound document performance.

## Use Batch Relationship Operations

When fetching multiple primary resources (e.g., `GET /users`), the framework resolves each user's relationships. By default, this means N separate calls — one per user, per relationship.

`BatchReadToManyRelationshipOperation` and `BatchReadToOneRelationshipOperation` replace N calls with a single batch call. Implement the `readBatches()` method:

```java
public class UserCitizenshipsOperations implements
        ToManyRelationshipOperations<UserDbEntity, DownstreamCountry>,
        BatchReadToManyRelationshipOperation<UserDbEntity, DownstreamCountry> {

    // Standard single-user read (used for GET /users/{id}/relationships/citizenships)
    @Override
    public PaginationAwareResponse<DownstreamCountry> readMany(JsonApiRequest request) {
        List<String> citizenshipIds = userDb.getUserCitizenships(request.getResourceId());
        return PaginationAwareResponse.inMemoryCursorAware(
            countriesClient.fetchByIds(citizenshipIds)
        );
    }

    // Batch read for all users at once (used for GET /users when resolving relationships)
    @Override
    public Map<UserDbEntity, PaginationAwareResponse<DownstreamCountry>> readBatches(
            JsonApiRequest request,
            List<UserDbEntity> users) {
        // 1. Collect all citizenship IDs across all users
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, List<String>> citizenshipsPerUser = userDb.getUsersCitizenships(userIds);

        // 2. Fetch all countries in one call
        List<String> allCountryIds = citizenshipsPerUser.values().stream()
            .flatMap(Collection::stream).distinct().toList();
        Map<String, DownstreamCountry> countries = countriesClient.fetchByIds(allCountryIds)
            .stream().collect(Collectors.toMap(DownstreamCountry::getCca2, c -> c));

        // 3. Map back to each user
        Map<String, UserDbEntity> usersById = users.stream()
            .collect(Collectors.toMap(UserDbEntity::getId, u -> u));
        return citizenshipsPerUser.entrySet().stream().collect(Collectors.toMap(
            e -> usersById.get(e.getKey()),
            e -> PaginationAwareResponse.inMemoryCursorAware(
                e.getValue().stream().map(countries::get).filter(Objects::nonNull).toList()
            )
        ));
    }
}
```

**Impact:** Fetching 50 users with citizenships goes from 50 downstream calls to 2 (one to load all citizenship IDs, one to fetch all countries).

The same pattern applies to to-one relationships via `BatchReadToOneRelationshipOperation`:

```java
public class UserPlaceOfBirthOperations implements
        ToOneRelationshipOperations<UserDbEntity, DownstreamCountry>,
        BatchReadToOneRelationshipOperation<UserDbEntity, DownstreamCountry> {

    @Override
    public Map<UserDbEntity, DownstreamCountry> readBatches(JsonApiRequest request,
                                                            List<UserDbEntity> users) {
        // Collect distinct country IDs, fetch once, map back
        Map<String, String> placeOfBirthPerUser = userDb.getUsersPlaceOfBirth(
            users.stream().map(UserDbEntity::getId).collect(Collectors.toSet())
        );
        Map<String, DownstreamCountry> countries = countriesClient.fetchByIds(
            placeOfBirthPerUser.values().stream().distinct().toList()
        ).stream().collect(Collectors.toMap(DownstreamCountry::getCca2, c -> c));

        return placeOfBirthPerUser.entrySet().stream().collect(Collectors.toMap(
            e -> usersById.get(e.getKey()),
            e -> countries.get(e.getValue())
        ));
    }
}
```

Once you implement a batch operation, the framework uses it for all scenarios — there is no need to also implement the single-resource operation separately.

## Leverage In-House Relationship Resolution

Sometimes your parent resource DTO already contains the relationship data. For example, `UserDbEntity` might hold a `placeOfBirthCountryCode` field. Instead of making a separate downstream call to resolve the relationship, you can extract it directly.

Override `readManyForResource()` or `readOneForResource()` to resolve relationships from the parent DTO in memory:

```java
public class UserPlaceOfBirthOperations implements
        ToOneRelationshipOperations<UserDbEntity, DownstreamCountry> {

    // Called when resolving relationships during parent resource reads
    @Override
    public DownstreamCountry readOneForResource(JsonApiRequest request, UserDbEntity user) {
        // Resolve directly from the parent DTO — no downstream call needed
        String countryCode = user.getPlaceOfBirthCountryCode();
        return countryCode != null ? new DownstreamCountry(countryCode) : null;
    }

    // Called for direct relationship endpoint: GET /users/{id}/relationships/placeOfBirth
    @Override
    public DownstreamCountry readOne(JsonApiRequest request) {
        String countryCode = userDb.getUserPlaceOfBirth(request.getResourceId());
        return countriesClient.fetchById(countryCode);
    }
}
```

This eliminates downstream calls entirely for relationships where the linkage data is already available in the parent model.

## Tune the Executor

JsonApi4j uses an `Executor` for parallel relationship resolution. When a resource has multiple relationships, they are resolved concurrently. The default is synchronous execution (`Runnable::run`).

Provide a custom `Executor` bean to enable parallelism:

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="exec-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="exec-quarkus">Quarkus</button>
  </div>
  <div id="exec-springboot" class="tab-panel active">
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="nd">@Configuration</span>
<span class="kd">public</span> <span class="kd">class</span> <span class="nc">JsonApi4jConfig</span> <span class="o">{</span>

    <span class="nd">@Bean</span>
    <span class="kd">public</span> <span class="nc">ExecutorService</span> <span class="nf">jsonApi4jExecutorService</span><span class="o">()</span> <span class="o">{</span>
        <span class="c1">// Virtual threads (Java 21+) — lightweight, ideal for I/O-bound work</span>
        <span class="k">return</span> <span class="nc">Executors</span><span class="o">.</span><span class="na">newVirtualThreadPerTaskExecutor</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
  <div id="exec-quarkus" class="tab-panel">
    <div class="language-java highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="kd">public</span> <span class="kd">class</span> <span class="nc">JsonApi4jConfig</span> <span class="o">{</span>

    <span class="nd">@Produces</span>
    <span class="nd">@Singleton</span>
    <span class="kd">public</span> <span class="nc">ExecutorService</span> <span class="nf">jsonApi4jExecutorService</span><span class="o">()</span> <span class="o">{</span>
        <span class="k">return</span> <span class="nc">Executors</span><span class="o">.</span><span class="na">newVirtualThreadPerTaskExecutor</span><span class="o">();</span>
    <span class="o">}</span>
<span class="o">}</span></code></pre></div></div>
  </div>
</div>

Common strategies:

| Executor | Best for |
|----------|----------|
| `Runnable::run` (default) | Simple APIs with few relationships |
| `Executors.newFixedThreadPool(N)` | Predictable concurrency with bounded threads |
| `Executors.newCachedThreadPool()` | Dynamic scaling for variable workloads |
| `Executors.newVirtualThreadPerTaskExecutor()` | I/O-bound relationship resolution (Java 21+) |

Parallelism helps most when a resource has multiple relationships that each trigger downstream calls. If relationships are resolved in-house (see above), the overhead of thread scheduling may outweigh the benefit.

## Limit Compound Document Depth

The `?include` parameter supports multi-level traversal (e.g., `?include=orders.lineItems.product`). Each level multiplies the number of downstream requests. Set limits to prevent unbounded resolution:

```yaml
jsonapi4j:
  cd:
    maxHops: 2              # Maximum relationship nesting depth
    maxIncludedResources: 50 # Maximum total included resources per response
```

| Property | Default | Effect |
|----------|---------|--------|
| `maxHops` | 3 | Limits `?include=a.b.c` depth. A value of 2 means `a.b` works but `a.b.c` stops at `b`. |
| `maxIncludedResources` | 100 | Caps the total number of resolved included resources. Prevents a single request from triggering thousands of downstream calls. |

For APIs with deep relationship graphs, start with `maxHops: 1` and increase only if clients need deeper traversal.

## Enable Compound Document Caching

The compound document resolver includes an in-memory LRU cache that stores individual resolved resources. When the same resource is included across multiple requests, it is served from cache instead of fetching again.

```yaml
jsonapi4j:
  cd:
    cache:
      enabled: true
      maxSize: 1000    # Maximum number of cached resource entries
```

Cache keys include the resource type, ID, downstream includes, and sparse fieldsets — so `GET /users/1?include=orders` and `GET /users/1?include=orders&fields[orders]=total` are cached separately.

The cache respects `Cache-Control` headers from downstream responses. Resources with `no-store` or `no-cache` directives are not cached. TTL is derived from `max-age` or `s-maxage`.

### Custom Cache Implementation

For distributed deployments, replace the built-in in-memory cache with a custom implementation (e.g., Redis). Extend `AbstractCompoundDocsResourceCache`, which enforces cacheability checks before storing:

```java
public class RedisCompoundDocsCache extends AbstractCompoundDocsResourceCache {

    private final RedisTemplate<String, String> redis;

    @Override
    public Optional<CacheResult> get(CacheKey key) {
        String json = redis.opsForValue().get(key.toString());
        if (json == null) return Optional.empty();
        Long ttl = redis.getExpire(key.toString(), TimeUnit.SECONDS);
        return Optional.of(new CacheResult(json, CacheControlDirectives.ofMaxAge(ttl)));
    }

    @Override
    protected void doPut(CacheKey key, String resourceJson, CacheControlDirectives directives) {
        long ttl = directives.effectiveMaxAge();
        redis.opsForValue().set(key.toString(), resourceJson, ttl, TimeUnit.SECONDS);
    }

    // Override getAll/putAll for batch efficiency with Redis MGET/MSET
    @Override
    public Map<CacheKey, CacheResult> getAll(Collection<CacheKey> keys) {
        // Use Redis MGET for batch retrieval
    }
}
```

Register the custom cache as a bean, and it replaces the built-in implementation automatically.

## Summary

| Optimization | Impact | When to use |
|-------------|--------|-------------|
| Bulk reads (`filter[id]`) | High | Always — required for efficient compound docs |
| Batch relationship operations | High | APIs serving list endpoints with relationships |
| In-house relationship resolution | Medium | When parent DTOs contain relationship data |
| Executor tuning | Medium | Resources with multiple relationships and I/O-bound resolution |
| Compound doc limits | Safety | Always — prevents runaway resolution |
| Compound doc caching | Medium | Repeated requests for the same included resources |
