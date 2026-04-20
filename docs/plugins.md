---
title: "Plugin System"
permalink: /plugins/
---

### Overview

The Plugin System provides an extension mechanism for **JsonApi4j** framework that allows developers to hook into the request processing pipeline and enrich or mutate the JSON:API request processing stages without modifying core logic.

Plugins can declare their specific additional metadata by decorating the different JSON:API elements:
* Operations (e.g. read, create, update, delete)
* Resources
* Relationships

Usually, the needed settings are specified via custom annotations that then can be parsed at runtime.

At runtime, **JsonApi4j** discovers and invokes registered plugins, asking each plugin to extract plugin-specific information from the current operation, resource, or relationship. This information is then passed downstream to consumers that understand the plugin's domain (for example, an Access Control evaluator).

The plugin system is:
* **Non-intrusive** – core execution flow remains unchanged
* **Annotation-driven** – plugins typically read metadata from annotations
* **Composable** – multiple plugins can coexist and contribute independently
* **Type-safe** – plugin contracts are defined via well-known interfaces
* **Visitor-based and flexible** – each plugin explicitly declares the list of visitor points it implements, making it clear where it can enrich, mutate, or intentionally short-circuit (break) JSON:API request processing

In short, plugins allow **JsonApi4j** to stay minimal and focused, while enabling powerful, opt-in extensions such as OpenAPI schema generation, security policies, and documentation tooling.

### Plugin System Architecture

The Plugin System is built around a pull-based extension model with visitor-driven consumption.

Plugins do not change the execution flow of **JsonApi4j**.
Instead, **JsonApi4j** collects plugin-specific information during request processing and exposes it to visitors, which apply that information to a concrete concern (for example, access control enforcement).

In other words:
* Plugins extract metadata
* Visitors interpret and act on it

### Request Processing Pipeline

Plugins hook into the [Request Processing Pipeline](/request-processing-pipeline/) at four defined stages using the Visitor pattern. For a detailed description of each pipeline stage, see the [Request Processing Pipeline](/request-processing-pipeline/) article.

![Request Processing Pipeline](/assets/images/request-processing-pipeline.svg "Request Processing Pipeline"){: .align-center}

Each plugin explicitly declares which visitor points it implements. At each plugin-enabled stage, a plugin visitor can return one of the following outcomes:

| Outcome | Effect |
|---|---|
| **`DO_NOTHING`** | No effect — the pipeline continues to the next stage |
| **`RETURN_DOC`** | Immediately finishes the pipeline and returns the provided document as the response |
| **`MUTATE_REQUEST`** | Modifies the current request context before proceeding (available at pre-retrieval stages) |
| **`MUTATE_DOC`** | Modifies the current document before proceeding (available at post-retrieval stages) |

This design allows plugins to operate at well-defined points without altering the core processing logic — keeping the framework minimal while enabling powerful extensions like access control enforcement or sparse fieldsets filtering. 

### Examples

Please refer:
* `JsonApiAccessControlPlugin`
* `JsonApiSparseFieldsetsPlugin`
* `JsonApiOasPlugin`
* `JsonApiCompoundDocsPlugin`

These are the plugins that are available for usage by default. If you're using JsonApi4j in terms of Spring Boot or Quarkus frameworks - you can just add the corresponding plugin dependency - and the plugin will be automatically integrated into the system.

In case most or all of the plugins are needed - just add one dependency that aggregates them all:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-all-plugins</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```
