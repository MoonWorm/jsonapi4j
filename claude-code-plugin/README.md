# jsonapi4j — Claude Code plugin

A [Claude Code](https://claude.com/claude-code) plugin that teaches AI agents how to **build apps with
the [jsonapi4j](https://api4.pro/) framework** (`pro.api4`). It bundles the `jsonapi4j` skill:
resources, to-one/to-many relationships, operations, compound documents/includes, pagination,
validation, access control, configuration, and RestAssured testing.

It auto-activates whenever you work in a codebase that depends on jsonapi4j (imports under
`pro.api4.jsonapi4j`, the `@JsonApiResource` / `@JsonApiRelationship` / `@JsonApiResourceOperation`
annotations, or a `/jsonapi` rootPath).

> This is for **developers building on** jsonapi4j. If you're contributing to the framework itself, see
> the repo's [`AGENTS.md`](../AGENTS.md) instead.

## Install

From any project that uses jsonapi4j:

```
/plugin marketplace add MoonWorm/jsonapi4j
/plugin install jsonapi4j@jsonapi4j
```

(The first command registers this repo as a plugin marketplace; the second installs the plugin from it.)

## What's inside

```
skills/jsonapi4j/
  SKILL.md            # lean entry point: mental model + new-resource checklist + reference index
  reference/          # deep dives, loaded on demand
    resources-and-operations.md   relationships.md        compound-documents.md
    performance.md                validation-and-security.md   configuration.md
    testing.md                    separation-of-concerns.md    known-behaviors.md
```

The skill points at real, runnable reference code in the framework's `examples/` sample apps and at the
docs on https://api4.pro/.

## License

Apache-2.0 — same as the framework. See the repository [`LICENSE`](../LICENSE).
