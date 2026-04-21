---
title: "Sparse Fieldsets Plugin"
permalink: /sparse-fieldsets-plugin/
---

The Sparse Fieldsets plugin implements [JSON:API Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets), allowing clients to request only the fields they need using the `fields[TYPE]=field1,field2` query parameter. This reduces payload size and improves response efficiency.

The plugin works automatically — no code changes are needed. Once enabled, the framework filters attributes on the server before serialization.

To enable the plugin, add the following dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-sf-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

### Usage

Request `users` with only `email` and `lastName` fields, and related `countries` with only `name`:

`GET /users?include=citizenships&fields[users]=email,lastName&fields[countries]=name`

### Edge Cases

| Scenario | Behavior |
|----------|----------|
| No `fields[TYPE]` parameter | All fields are returned for that resource type |
| Empty value: `fields[users]=` | No fields are returned for that resource type |
| Non-existing type or field | Ignored |
| Nested path where parent exists but leaf doesn't (e.g. `a.b.c` where `a.b` exists but `c` doesn't) | Entire path is ignored |
| All requested fields don't exist | Treated as empty fields (no fields returned) by default. Configurable via `requestedFieldsDontExistMode`. |
| Primitive-typed fields (e.g. `int`) | Cannot be excluded if other fields at the same level are requested, because primitives can't be set to `null`. Use object types (e.g. `Integer`) for full sparse fieldsets support. |
| Multi-type responses (e.g. users + included countries) | Use separate `fields[TYPE]` parameters per resource type |

### Available Properties

| Property name                               | Default value | Description                                                                                                             |
|---------------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| `jsonapi4j.sf.enabled`                      | `true`          | Enables/disables Sparse Fieldsets plugin                                                                                |
| `jsonapi4j.sf.requestedFieldsDontExistMode` | `SPARSE_ALL_FIELDS`  | Behavior when all requested fields don't exist. Options: `SPARSE_ALL_FIELDS` (return no fields), `RETURN_ALL_FIELDS` (return all fields) |
