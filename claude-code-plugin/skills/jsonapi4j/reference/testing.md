# Testing (RestAssured black-box)

Pattern from the framework's own `examples/`: boot the real app, drive live HTTP with RestAssured,
assert on the JSON:API document shape. The sample apps share one suite
(`examples/jsonapi4j-sampleapp-testsuite`) run by all three integrations (Spring Boot / Quarkus /
Servlet) — mirror that structure.

- **Ordinary endpoints**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, set `RestAssured.port` from
  `@LocalServerPort` in `@BeforeEach`. Send `Accept` / `Content-Type: application/vnd.api+json`.
- **Compound-docs (`?include=`) tests**: CD's self-HTTP needs a *known* port, so use
  `@SpringBootTest(webEnvironment = DEFINED_PORT)` with `server.port` pinned (so `cd.mapping`'s
  `${server.port}` matches the live server) + `@DirtiesContext` to release the port. RANDOM_PORT
  (`server.port=0`) makes includes unresolvable.
- **Layer test profiles**: keep CD **disabled** in the plain profile, enable it only in the CD profile
  (compose `@ActiveProfiles({"test","cdtest"})` to reuse the base config and only override port +
  `cd.enabled`).
- **Data**: Testcontainers (e.g. Postgres) + Flyway; mock external integrations (`@MockitoBean`) so no
  test hits the network. For auth, override `JwtDecoder` with a stub that reads the bearer token as the
  principal id.
- **Assertions**: `data.id`, `data.type`, `data.attributes.X`, `data.relationships.X.data` (present for
  to-one only with `?include`; without include you get only `links`), `included.find { it.type == 'X' }`.
- **Multi-hop**: assert both the linkage and that each hop's resource lands in `included`.

---

**Canonical examples in the framework**
- Shared suite: `examples/jsonapi4j-sampleapp-testsuite/.../CompoundDocsOperationsTests.java`,
  `AccessControlOperationsTests.java`, `SparseFieldsetsOperationsTests.java`
- Per-app: `examples/jsonapi4j-springboot-sampleapp/.../operations/Spring*Tests.java` (+ Quarkus /
  Servlet equivalents)
- Docs: https://api4.pro/request-response-examples/
