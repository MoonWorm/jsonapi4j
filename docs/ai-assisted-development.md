---
title: "AI-Assisted Development"
permalink: /ai-assisted-development/
---

JsonApi4j is built to work well with AI coding agents. The repository ships two first-class,
version-controlled assets — one for people **building apps with** the framework, and one for people
**contributing to** it — so your AI assistant has accurate, framework-specific context instead of
guessing from generic Java knowledge.

### Building apps with the framework — the Claude Code plugin

The project publishes an official **[Claude Code](https://claude.com/claude-code) plugin** that bundles
the `jsonapi4j` skill. Once installed, your agent knows how to model and build a JSON:API service the
framework's way.

Install it from any project that depends on JsonApi4j:

```text
/plugin marketplace add MoonWorm/jsonapi4j
/plugin install jsonapi4j@jsonapi4j
```

The first command registers this repository as a plugin marketplace; the second installs the plugin
from it. The skill **auto-activates** whenever you work in a codebase that uses JsonApi4j — detected
from imports under `pro.api4.jsonapi4j`, the `@JsonApiResource` / `@JsonApiRelationship` /
`@JsonApiResourceOperation` annotations, or a `/jsonapi` root path.

It covers the full surface a typical service needs:

- **Resources, relationships, and operations** — the three-part anatomy, lightweight relationship refs
  vs. full DTOs, and the composite operation interfaces.
- **Compound documents** — `?include=` resolution, `cd.mapping`, and multi-hop traversal.
- **Performance** — N+1-safe relationship resolution, batch operations, and `filter[id]` batching.
- **Validation, access control, and configuration** — the fluent validators, per-field authorization,
  and the `jsonapi4j.*` properties.
- **Testing** — black-box RestAssured patterns, including the port setup that compound-document tests
  require.

The skill uses progressive disclosure — a lean entry point plus on-demand reference files — and points
at real, runnable code in the framework's [sample apps](https://github.com/MoonWorm/jsonapi4j/tree/main/examples)
and the pages on this site. You don't need to check out the framework source: add the dependency, install
the plugin, and build in your own repository.

### Contributing to the framework — AGENTS.md

For working **on** JsonApi4j itself, the repository root carries a committed
[`AGENTS.md`](https://github.com/MoonWorm/jsonapi4j/blob/main/AGENTS.md) (with a nested
[`docs/AGENTS.md`](https://github.com/MoonWorm/jsonapi4j/blob/main/docs/AGENTS.md) for documentation-site
work). [`AGENTS.md`](https://agents.md/) is an emerging cross-tool standard, so the same context is
picked up by Claude Code, Cursor, GitHub Copilot, Codex, and other agents.

It is a lean router, not a manual — it gives an agent the few things it can't cheaply derive:

- **Build, test, and verify commands** — including the policy of keeping the shared test suite green
  across all three sample apps (Spring Boot, Quarkus, Servlet).
- **A module map** and the dependency direction.
- **Load-bearing conventions** — commit format, the extension points (overridable default beans, the
  plugin SPI), and a handful of non-obvious gotchas.
- **Pointers** to the deeper docs, so it knows where to read more.

### Why it matters

The value isn't "the AI writes your API for you." It's that an agent equipped with this context produces
**consistent, spec-compliant code that follows the framework's idioms** — lightweight relationship refs
by default (while leaving room for fuller DTOs where they fit), includes wired through `cd.mapping`,
N+1-safe resolution, and the correct test setup —
rather than plausible-looking Java that fights the framework. Less review friction, fewer subtle bugs.

### Links

- Plugin source: [`claude-code-plugin/`](https://github.com/MoonWorm/jsonapi4j/tree/main/claude-code-plugin)
- Contributor context: [`AGENTS.md`](https://github.com/MoonWorm/jsonapi4j/blob/main/AGENTS.md)
- Repository: [github.com/MoonWorm/jsonapi4j](https://github.com/MoonWorm/jsonapi4j)
