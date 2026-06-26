# Separation of concerns (attribute ownership)

Each resource's attributes must contain **only its own data**. Smells to flag/fix:

- A foreign-key id as an *attribute* → make it a **relationship** instead.
- Another resource's fields flattened in (e.g. `showTitle`, `showSlug` on an episode) → expose via a
  relationship + `?include`, not denormalized attributes.
- Cross-resource **aggregates** computed onto a parent (e.g. a show's `nextEpisodeDate` from its
  episodes) are borderline-acceptable as summary data — name them clearly; they're not strict ownership
  violations.
- A join-entity's own data (e.g. a subscription's `muted`) with nowhere to live → either
  relationship-identifier `meta` (lightweight) or promote to a first-class resource (see
  `relationships.md`).

## Migrating a denormalized field out (strip attribute → expose via `include`)

Do it carefully:

1. Add the relationship (often a cheap in-house to-one off a FK the DTO already carries), then drop the
   field from the *attributes class + resource resolver* — but **keep it on the DTO** if internal code
   (digests, notifications, sorting) still reads it. (The DTO may carry more than the resource exposes.)
2. **Confirm every path that produces the resource can resolve the relationship** before removing the
   field. A synthetic/non-persisted variant (e.g. a composed item with no FK for that hop) can't be
   `include`d, so a field only reachable that way must stay (see `compound-documents.md`).
3. It's a **breaking wire change**: existing clients reading the attribute now get null. Migrate
   consumers to read it from `included` (match by `data.relationships.<rel>.data.id`) **first**, then
   strip — and mind independent deploy order (ship frontends before the API, or accept a breakage
   window).
4. Update tests: drop the old assertions on the removed field (don't bother asserting it's now `null` —
   that just pins a permanent reality); the meaningful new coverage is an `?include=<rel>` test proving
   the neighbour resolves.

---

**Docs**: https://api4.pro/domain/ · https://api4.pro/spec-deviations/
