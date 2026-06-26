# Known framework behaviors / quirks

Behaviors are version-specific (versions noted where known; observed on the 1.8.x line). **Verify
against the version on your classpath and the framework's own tests** — this is the maintainer's own
evolving library, so treat edge behaviors as "confirm, don't assume."

- **Related-resource URL returns 404**: `/{type}/{id}/{relName}` (e.g. `/users/{id}/citizenships`) is
  NOT served — only `/{type}/{id}/relationships/{relName}` (linkage) and `?include=` work.
- **To-one without `?include`** renders only `relationships.<rel>.links` (no `data` linkage); the
  linkage `data` appears with `?include` or on the standalone `/relationships/<rel>` endpoint.
- **Optional to-one — a null ref is fine** (verified @1.8.4). Returning `null` from
  `readOne`/`readOneForResource` is valid and does NOT 502. Rendering differs by shape: a
  **single-resource** read with `?include` emits `relationships.<rel>.data: null`; in a **collection**
  read with `?include` the no-target rows omit `data` entirely (only `links`). Clients should handle
  both — `rel && rel.data` covers it.
- **Cross-user resource read isn't auto-403**: AC ownership is field-level; a non-owner GET returns 200
  with full attributes unless an attribute is explicitly owner-gated.
- **Cursor**: the next-page cursor is at top-level meta under the flat key
  `meta."pagination.nextCursor"` (absent/null = last page).
- **Enum-typed attribute fields** — fine from 1.8.4; broke on ≤1.8.3. You can use a Java `enum` directly
  as an `*Attributes` field (Jackson serializes it as its `name()`, e.g. `"ACTIVE"`). On ≤1.8.3 the AC
  plugin's outbound nested-class walker recursed into the enum's self-referential constants → infinite
  recursion → 500. If stuck on ≤1.8.3, expose the enum as its `String` name (keep the enum on the DTO).
- **Type/filename mismatch cascade**: a public top-level record/type must match its filename (basic Java
  rule), but a mismatch cascades into a wall of "cannot find symbol" Lombok-getter errors elsewhere —
  look past those for the real cause.

When you confirm a new behavior, add it here with the version observed.

---

**Docs**: https://api4.pro/spec-deviations/
