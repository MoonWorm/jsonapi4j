# JSON:API 1.1 Compliance Matrix

This matrix tracks JSON:API 1.1 parity for **JsonApi4j** server responsibilities in scope for the compliance program.
Scope is intentionally limited to runtime request/response behavior and framework-level validation/error semantics.

Spec reference: [jsonapi.org/format](https://jsonapi.org/format/)

## Scope and Defaults

- Default behavior: `jsonapi4j.compatibility.legacyMode=false` (`STRICT`)
- Migration switch: `jsonapi4j.compatibility.legacyMode=true` (`LEGACY`)
- Out of scope: [Atomic Operations](https://jsonapi.org/ext/atomic/)

Example:

```yaml
jsonapi4j:
  compatibility:
    legacyMode: false
    supportedExtensions:
      - "https://example.com/ext/bulk"
    supportedProfiles:
      - "https://example.com/profile/audit"
```

## Compliance Matrix

| JSON:API 1.1 requirement | Level | Strict mode behavior | Legacy mode behavior | Status |
| --- | --- | --- | --- | --- |
| [JSON:API media type and parameters in `Content-Type`](https://jsonapi.org/format/#content-negotiation) | MUST | Accepts only JSON:API media type with allowed parameters and valid URI-list values for `ext` / `profile`; invalid/unsupported cases return `415`. | Tolerant parsing preserved for migration. | Implemented |
| [Accept negotiation for JSON:API media type instances](https://jsonapi.org/format/#content-negotiation) | MUST | Returns `406` when all acceptable JSON:API instances are invalid/unsupported; honors wildcards and quality values. | Tolerant parsing preserved for migration. | Implemented |
| [Response `Content-Type` for JSON:API documents](https://jsonapi.org/format/#content-negotiation-servers) | MUST | Emits `application/vnd.api+json` without extra media type parameters for JSON:API payloads. | Same behavior. | Implemented |
| [Create success semantics](https://jsonapi.org/format/#crud-creating-responses-201) | MUST | Resource create `POST` defaults to `201`. | Same behavior (`201`). | Implemented |
| [Synchronous update/delete semantics](https://jsonapi.org/format/#crud-updating-responses-204) | MUST | Resource `PATCH`/`DELETE` and relationship linkage mutation defaults are synchronous `204`. | Preserves historical mutation status behavior (`202`). | Implemented |
| [To-many relationship linkage mutation endpoints](https://jsonapi.org/format/#crud-updating-to-many-relationships) | MUST (when operation is supported) | Routes and executes `PATCH`/`POST`/`DELETE` for `/relationships/{toMany}` when corresponding operations are registered. | Same behavior. | Implemented |
| [Identity conflict rules for create/update](https://jsonapi.org/format/#crud-updating-responses-409) | MUST | Enforces request identity rules and returns `409` for type/id mismatches. | Same core behavior; strict-only checks remain strict-mode gated where applicable. | Implemented |
| [Relationship payload shape and unsupported-semantics handling](https://jsonapi.org/format/#crud-updating-relationships) | MUST | Validates linkage payload shape and returns JSON:API-aligned `400` / `403` / `409`. | Same core behavior; strict-only checks remain strict-mode gated where applicable. | Implemented |
| [Include path validation](https://jsonapi.org/format/#fetching-includes) | MUST | Invalid include paths are rejected at request boundary in strict mode. | Include path validation can be relaxed for compatibility. | Implemented |
| [Error document semantics](https://jsonapi.org/format/#errors) | MUST | Preserves operation-level exceptions and maps invalid payload/cursor errors to `400` JSON:API error docs. | Same behavior. | Implemented |
| [Sparse fieldsets `fields[TYPE]`](https://jsonapi.org/format/#fetching-sparse-fieldsets) | MAY | Parses and applies sparse fieldsets to primary and included resources. | Same behavior. | Implemented |
| [Resource local identifiers `lid`](https://jsonapi.org/format/#document-resource-object-identification) | MAY | Supports `lid` in resource objects and resource identifiers for local-id workflows. | Same behavior. | Implemented |
| [Profiles and extensions capability model](https://jsonapi.org/format/#content-negotiation) | MUST / SHOULD | Unsupported requested `ext` URIs are rejected in strict mode when not configured in `supportedExtensions`; unknown profiles are ignored per spec rules. | Tolerant parsing preserved for migration. | Implemented |

## Compatibility and Feature Flags

| Property | Purpose | Default |
| --- | --- | --- |
| `jsonapi4j.compatibility.legacyMode` | Enables migration behavior (`LEGACY`) instead of strict JSON:API 1.1 defaults. | `false` |
| `jsonapi4j.compatibility.supportedExtensions` | Declares supported extension URIs used for strict `ext` negotiation checks. | `[]` |
| `jsonapi4j.compatibility.supportedProfiles` | Declares profile URIs for documentation/forward compatibility; unknown requested profiles are ignored per JSON:API 1.1. | `[]` |

## Release Validation

Release validation commands:

1. Full multi-module build verification from repo root:
   - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -B verify`
2. Sample app smoke verification:
   - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -B -pl examples/jsonapi4j-springboot-sampleapp -am test`

Latest run results (2026-02-27):

| Command | Result |
| --- | --- |
| `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -B verify` | PASS |
| `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -B -pl examples/jsonapi4j-springboot-sampleapp -am test` | PASS |
