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
```

- Modules are named by directory, e.g. `-pl jsonapi4j-core` or `-pl examples/jsonapi4j-springboot-sampleapp`.
- **Verification policy:** there are **three sample apps** — Spring Boot, Quarkus, and Servlet
  (`examples/jsonapi4j-*-sampleapp`) — sharing one domain (`examples/jsonapi4j-sampleapp-domain`)
  and one test suite (`examples/jsonapi4j-sampleapp-testsuite`). Each app runs the shared suite via
  per-app subclasses (e.g. `Spring`/`Quarkus`/`Servlet` + `CompoundDocsOperationsTests`). Any
  framework change must keep **all three apps' tests green** — that's the end-to-end guarantee that
  behavior is identical across every integration.
- CI builds on push to `main` and on PRs; `docs/**`-only changes are skipped via `paths-ignore`.

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
