---
title: "Configuration"
permalink: /configuration/
---

## JsonApi4j properties

Here is the list of the framework core properties:

| Property name          | Default value | Description                                     |
|------------------------|---------------|-------------------------------------------------|
| `jsonapi4j.rootPath` | `/jsonapi`          | Sets the root path for all JsonApi4j operations |

### Plugin-specific properties

Each plugin has its own configuration properties. Refer to the corresponding plugin page for details:

- [Access Control Plugin](/access-control-plugin/#available-properties) — `jsonapi4j.ac.*`
- [Sparse Fieldsets Plugin](/sparse-fieldsets-plugin/#available-properties) — `jsonapi4j.sf.*`
- [OpenAPI Plugin](/openapi-plugin/#available-properties) — `jsonapi4j.oas.*`
- [Compound Documents Plugin](/compound-docs-plugin/#available-properties) — `jsonapi4j.cd.*`
