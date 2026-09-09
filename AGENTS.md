# jsonapi4j — Agent Guide

Persistence-agnostic Java framework for building REST APIs that comply with the
[JSON:API spec](https://jsonapi.org/). Bring any data source (SQL, NoSQL, REST, in-memory) —
no JPA/Hibernate. Integrates with **Spring Boot**, **Quarkus**, and the plain **Jakarta Servlet API**.

This file is the canonical context for AI coding agents (read by Claude Code, Cursor, Copilot,
Codex, and others). Keep it lean — it loads on every turn. Put deep reference material in the
sources it points to, not here.

## Coordinates

- **groupId:** `pro.api4` · **Apache 2.0** · maintainer: Aliaksei Taliuk
- **Version:** single source of truth is `<revision>` in the root `pom.xml`. Bump it there only.
- **Java release:** see `<maven.compiler.release>` in the root `pom.xml` (currently 23).
- **Docs:** https://api4.pro · **Repo:** https://github.com/MoonWorm/jsonapi4j
- **Canonical references** (don't duplicate them here): root `README.md`, the `docs/*.md` pages,
  and per-module `README.md` files.

## Build, test & verify

```bash
mvn clean install                 # full build + tests
mvn -pl <module> -am test         # build one module (and its deps), run its tests
mvn -pl <module> test -Dtest=SomeClassTests           # single test class
mvn -pl <module> test -Dtest=SomeClassTests#methodName  # single test method

mvn clean verify -Pspring-boot-4  # same sources against Spring Boot 4 (see below)
```

`jsonapi4j-rest-springboot` is one artifact supporting **Spring Boot 3 and 4**. The default build is the
published baseline (Boot 3.4.x — compiling against the floor keeps a Boot-4-only API a compile error rather
than a runtime failure for Boot 3 users). The `spring-boot-4` profile re-runs the same sources against Boot
4.1, Spring Security 7 and springdoc 3. The test stack is identical on both axes, so a failure there is
attributable to Spring rather than to a swapped test library. **Both must be green.**

Pull requests build the Spring Boot 3 baseline only, to keep runner minutes down. Spring Boot 4 is covered by
the weekly `spring-boot-compatibility.yml` and hard-gated in `release.yml`, so a Boot 4 regression cannot reach
Maven Central — it just surfaces later than on the pull request itself. **Run `-Pspring-boot-4` locally before
pushing anything that touches `jsonapi4j-rest-springboot`**, because PR CI will not catch it for you.

A third combination — the artifact exactly as published (compiled against the Boot 3 floor) running on a Boot 4
runtime — is what a Boot 4 user actually gets from Maven Central, and neither matrix axis reproduces it, since
each axis recompiles the framework. It runs weekly in `spring-boot-compatibility.yml` and gates the release,
not every pull request: anything *removed* in Boot 4 already fails the Boot 4 axis at compile time, so this
only guards the rarer case of a source-compatible but binary-incompatible change. Reproduce locally with:

```bash
mvn -B clean install -DskipTests                                          # framework on the Boot 3 baseline
mvn -B -pl examples/jsonapi4j-springboot-sampleapp test -Pspring-boot-4   # no -am: resolves the installed jar
```

Two things about that profile, both easy to break:

- It is declared in `jsonapi4j-rest-springboot/pom.xml` and `examples/pom.xml`, **never in the root pom**.
  Maven drops a POM's `activeByDefault` profiles as soon as another profile in that same POM activates, and
  the root's `build-project` profile carries the surefire, source-jar and JaCoCo configuration.
- `spring.boot.version` is declared in both the root pom and `examples/pom.xml` (the examples aggregator is
  deliberately parentless and inherits nothing). The two must move together.
- Never pass `-Pspring-boot-4` to `deploy` — the published pom must advertise the 3.x floor.
- The RestAssured family (`rest-assured`, `json-path`, `xml-path`, `rest-assured-common`) is managed as a
  set in `examples/pom.xml`. The Spring Boot BOM manages some of those artifacts, so pinning only
  `rest-assured` leaves a split family that fails with `ClassNotFoundException` at runtime.
- **springdoc's major version is tied to the Spring Boot generation and must move with the profile.**
  springdoc 2.x is built against Boot 3; springdoc 3.x depends on `spring-boot-webmvc`,
  `spring-boot-web-server` and `spring-boot-health`, which exist only in Boot 4. Putting springdoc 3 on a
  Boot 3 application drags Boot 4 modules onto the classpath and the context fails with duplicate
  auto-configuration beans. The coupling is **one-way**: springdoc 2 on Boot 4 works (verified by hand —
  `/v3/api-docs` returns 200 on Boot 4.1.0 with springdoc 2.8.8), so the constraint is only "do not run
  springdoc 3 on Boot 3". The profile still moves springdoc up with Boot to keep the sample app on the
  generation each targets.
- springdoc is only used by the Spring **sample app**, never by the framework — no framework module imports
  `org.springdoc`. Nothing in the test suite exercises springdoc's own endpoints (`/v3/api-docs`,
  `swagger-ui`); `OasDocumentTests` targets jsonapi4j's own OAS servlet at `jsonapi4j.oas.oasRootPath`. So
  springdoc is verified to be classpath-compatible, not verified to work.

- Modules are named by directory, e.g. `-pl jsonapi4j-core` or `-pl examples/jsonapi4j-springboot-sampleapp`.
- **Verification policy:** there are **three sample apps** — Spring Boot, Quarkus, and Servlet
  (`examples/jsonapi4j-*-sampleapp`) — sharing one domain (`examples/jsonapi4j-sampleapp-domain`)
  and one test suite (`examples/jsonapi4j-sampleapp-testsuite`). Each app runs the shared suite via
  per-app subclasses (e.g. `Spring`/`Quarkus`/`Servlet` + `CompoundDocsOperationsTests`). Any
  framework change must keep **all three apps' tests green** — that's the end-to-end guarantee that
  behavior is identical across every integration.
- CI builds on push to `main` and on PRs; `docs/**`-only changes are skipped via `paths-ignore`.

### Writing tests

Tests are organized by **unit under test**, never by scenario:

- **One class per unit under test**, named `<ClassUnderTest>Tests`, in that class's package. Group
  scenarios with `@Nested` inner classes — not with extra top-level classes. Shared fixtures,
  `@AfterEach` cleanup, and helpers live on the outer class. See `JsonApiRequestValidatorTests`,
  `DefaultAccessControlEvaluatorTests`.
- **Name the subject `sut`:** `private final Foo sut = new Foo();`.
- **Method names are `methodUnderTest_condition_expectation`** — `merge_twoEqual_resultIsTheSame`,
  `enabled_disabledProperties_returnsFalse`. The name is the documentation.
- **No comments.** No `@Nested` Javadoc, no `// --- section ---` banners. Exception: `// given` /
  `// when` / `// then` markers when a section runs past 2-3 lines (`// when - then` when the call
  under test sits inside the assertion). A one-line setup plus one assertion gets nothing.
- **One given/when/then cycle per test.** Two setup→assert cycles in one method means two tests.

## Module map

Dependency direction flows downward; pick the integration module that matches the host stack.

```
jsonapi4j-base      domain interfaces, annotations, model, plugin SPI (minimal deps)
  └ jsonapi4j-core  processors, DomainRegistry / OperationsRegistry, JsonApi4j entry point
      └ jsonapi4j-rest                servlet integration (dispatcher, error handling, filters)
          ├ jsonapi4j-rest-springboot  Spring Boot auto-config
          └ jsonapi4j-rest-quarkus-parent  Quarkus runtime/ (CDI) + deployment/ (build-time gen)
jsonapi4j-compound-docs-resolver   technology-agnostic include resolution + HTTP cache
jsonapi4j-plugins/*                 optional: ac, cd, oas, sf, all-plugins
examples/*                          3 sample apps (Spring Boot / Quarkus / Servlet) + shared domain & test suite
```

See each module's `README.md` for its key classes and Maven snippet.

## Conventions that matter

Most style is discoverable from the code; these are the load-bearing, non-obvious rules:

- **Commits:** `#<issue-number> - <description>`.
- **Formatting:** run the default Java formatter before pushing. 4-space indent, K&R braces, ~120 col.
- **Generics:** descriptive UPPER_CASE type params (`<RESOURCE_DTO>`, not `<T>`); no raw types anywhere.
- **Extension points (for forks):** defaults are overridable beans — Spring `@ConditionalOnMissingBean`,
  Quarkus `@DefaultBean`. Override these (e.g. `ExecutorService`, `CompoundDocsResourceCache`,
  error handlers) rather than editing core. The plugin SPI (`JsonApi4jPlugin`, precedence-ordered
  phase visitors) is the other extension seam — see `docs/writing-a-custom-plugin.md`.
- **Gotchas:** JSON:API media type is `application/vnd.api+json` (enforced by content negotiation);
  the `x-disable-compound-docs` header (`HttpHeaders.X_DISABLE_COMPOUND_DOCS`) prevents recursive
  compound-docs resolution; the authenticated principal is request-scoped via a `ThreadLocal`
  (`AuthenticatedPrincipalContextHolder`).

## Where to find things

- **How to use the framework** (resources, relationships, operations, includes, validation,
  testing): browse `docs/` — start with `docs/getting-started.md`. Every topic page is listed and
  grouped (Getting Started · Framework Internals · Plugins · Advanced) in
  `docs/_data/navigation.yml`, the canonical index. (If your agent has the `jsonapi4j` Claude Code
  skill installed, it covers the same ground interactively.)
  - Topic aliases (where the page name isn't obvious): includes / compound docs →
    `compound-docs.md` + `compound-docs-plugin.md` · auth / security / permissions →
    `access-control-plugin.md` · filtering → `filtering-and-sorting.md` · errors →
    `error-handling.md`
- **Contributing:** `CONTRIBUTING.md` (fork → branch → PR; commit format above; run the formatter
  and `mvn clean install` before pushing).
- **Documentation site work:** `docs/AGENTS.md`.
- **Building an app *with* the framework** (a different audience than this file): that's the consumer
  Claude Code plugin in `claude-code-plugin/` (registered via `.claude-plugin/marketplace.json`). This
  `AGENTS.md` is for framework *contributors*; the plugin's skill is for framework *users*.
