---
title: "Plugin System"
permalink: /plugins/
---

### Overview

The Plugin System provides an extension mechanism for **JsonApi4j** that allows developers to hook into the request processing pipeline and enrich or mutate the JSON:API request processing stages without modifying core logic.

Plugins can declare additional metadata by decorating different JSON:API elements:
* Operations (e.g. read, create, update, delete)
* Resources
* Relationships

At runtime, **JsonApi4j** discovers and invokes registered plugins, asking each plugin to extract plugin-specific information from the current operation, resource, or relationship. This information is then passed downstream to consumers that understand the plugin's domain (for example, an Access Control evaluator). Settings are typically specified via custom annotations that are parsed at runtime.

The plugin system is:
* **Non-intrusive** – core execution flow remains unchanged
* **Composable** – multiple plugins can coexist and contribute independently
* **Visitor-based** – each plugin explicitly declares the list of visitor points it implements, making it clear where it can enrich, mutate, or short-circuit JSON:API request processing

### Built-in Plugins

JsonApi4j ships with four plugins. Each is a separate dependency — add only what you need:

| Plugin | Artifact | Description |
|--------|----------|-------------|
| [Access Control](/access-control-plugin/) | `jsonapi4j-ac-plugin` | Annotation-driven authorization with per-field anonymization based on authentication, access tier, OAuth2 scopes, and resource ownership |
| [OpenAPI](/openapi-plugin/) | `jsonapi4j-oas-plugin` | Auto-generates an OpenAPI specification from your declared domain |
| [Sparse Fieldsets](/sparse-fieldsets-plugin/) | `jsonapi4j-sf-plugin` | Implements `fields[type]` filtering to return only requested attributes |
| [Compound Documents](/compound-docs-plugin/) | `jsonapi4j-cd-plugin` | Resolves `include` queries with multi-level relationship chaining and parallel batch fetching |

To include all plugins as a single dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-all-plugins</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

If you're using Spring Boot or Quarkus, plugins on the classpath are auto-configured.

### Writing Your Own Plugin

You can build custom plugins that hook into the same pipeline stages as the built-in ones. See the [Writing a Custom Plugin](/writing-a-custom-plugin/) guide for a complete walkthrough — it builds a working Field Masking Plugin from scratch.

### Plugin Architecture

The Plugin System is built around a pull-based extension model with visitor-driven consumption.

Plugins do not change the execution flow of **JsonApi4j**.
Instead, **JsonApi4j** collects plugin-specific information during request processing and exposes it to visitors, which apply that information to a concrete concern (for example, access control enforcement).

In other words:
* Plugins extract metadata
* Visitors interpret and act on it

### Pipeline Integration

Plugins hook into the [Request Processing Pipeline](/request-processing-pipeline/) at four defined stages using the Visitor pattern. For a detailed description of each pipeline stage, see the [Request Processing Pipeline](/request-processing-pipeline/) page.

![Request Processing Pipeline](/assets/images/request-processing-pipeline.svg "Request Processing Pipeline"){: .align-center}

Each plugin explicitly declares which visitor points it implements. At each plugin-enabled stage, a plugin visitor can return one of the following outcomes:

| Outcome | Effect |
|---|---|
| **`DO_NOTHING`** | No effect — the pipeline continues to the next stage |
| **`RETURN_DOC`** | Immediately finishes the pipeline and returns the provided document as the response |
| **`MUTATE_REQUEST`** | Modifies the current request context before proceeding (available at pre-retrieval stages) |
| **`MUTATE_DOC`** | Modifies the current document before proceeding (available at post-retrieval stages) |

This design allows plugins to operate at well-defined points without altering the core processing logic — keeping the framework minimal while enabling powerful extensions like access control enforcement or sparse fieldsets filtering.
