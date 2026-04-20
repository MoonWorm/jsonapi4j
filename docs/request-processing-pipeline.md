---
title: "Request Processing Pipeline"
permalink: /request-processing-pipeline/
---

Every incoming JSON:API request in **JsonApi4j** flows through a well-defined processing pipeline. The pipeline is responsible for retrieving data, composing a spec-compliant JSON:API document, resolving relationships, and returning the final response.

Understanding the pipeline is essential for working effectively with the framework — it determines when your code runs, what data is available at each point, and how the final response is assembled.

### Resource Operations Pipeline

Resource operations (`ReadResourceByIdOperation`, `ReadMultipleResourcesOperation`, `CreateResourceOperation`, etc.) go through the full pipeline with seven stages:

![Request Processing Pipeline](/assets/images/request-processing-pipeline.svg "Request Processing Pipeline"){: .align-center}

#### 1. Pre-Retrieval

Before any data is fetched, the framework prepares the request context. This is the earliest point in the pipeline — the request has been parsed and validated, and the target resource type and operation have been resolved.

At this stage, registered [plugins](/plugins/) can inspect or mutate the request before any I/O occurs.

#### 2. Data Retrieval

The framework invokes your operation's main data retrieval method (e.g. `readById()`, `readPage()`, `create()`). This is typically the heaviest step — it's where your code talks to databases, external APIs, or any other data source.

If the operation returns `null` or an empty result, the pipeline short-circuits and immediately generates an empty JSON:API document with appropriate links and meta.

#### 3. Post-Retrieval

Once the raw data (your internal DTO) has been retrieved, the framework gives plugins a chance to inspect or act on it before document composition begins. The downstream data is available but hasn't been transformed into a JSON:API document yet.

#### 4. Compose JSON:API Response

The framework builds a spec-compliant JSON:API document from the retrieved data. This is a standardized step that runs automatically:

- **Resource ID and type** are resolved via `resolveResourceId()` and `@JsonApiResource(resourceType = ...)`
- **Attributes** are resolved via `resolveAttributes()`
- **Resource-level links** are generated (self link by default, customizable)
- **Resource-level meta** is resolved if implemented
- **Top-level links** are generated (self, pagination links for multi-resource responses)
- **Top-level meta** is resolved if implemented

At this point, the document contains the primary resource(s) without relationships — those come next.

#### 5. Relationships Pre-Retrieval

Before the framework fetches related data, plugins get another hook point. Since relationship resolution may involve additional I/O operations (more database queries, more API calls), this is an opportunity to short-circuit the pipeline or modify the request context.

#### 6. Fetch Relationship Data (Parallel)

The framework resolves all declared relationships concurrently. For each relationship defined on the resource:

- **To-many relationships** are resolved via `ToManyRelationshipOperations.readManyForResource()` (or the batch variant `readBatches()` for multi-resource responses)
- **To-one relationships** are resolved via `ToOneRelationshipOperations.readOneForResource()` (or `readBatches()`)

Relationship resolution runs in parallel using the configured `Executor`. For multi-resource responses (e.g. `GET /users`), batch relationship resolvers are preferred — they fetch relationship data for all resources in a single call, minimizing the number of downstream requests.

If a relationship operation is not implemented, the framework falls back to a default resolver that produces a relationship object with only a `self` link and no `data` so clients can see which relationships are available for resources.

#### 7. Relationships Post-Retrieval

The final stage. All relationships have been resolved and set on the document. The fully assembled JSON:API response is ready to be returned. Plugins get one last chance to inspect or mutate the document before it's serialized and sent to the client.

### Relationship Operations Pipeline

Relationship operations (`ReadToManyRelationshipOperation`, `ReadToOneRelationshipOperation`, etc.) follow a shorter pipeline. Since these operations target a specific relationship directly, there is no need for nested relationship resolution:

![Relationship Operations Processing Pipeline](/assets/images/request-processing-pipeline-relationships.svg "Relationship Operations Processing Pipeline"){: .align-center}

The pipeline consists of four stages:

1. **Pre-Retrieval** — the request is prepared; plugins can inspect or mutate it
2. **Data Retrieval** — the framework invokes your relationship operation (e.g. `readMany()`, `readOne()`). If the result is empty, an empty document is returned immediately
3. **Post-Retrieval** — plugins can inspect or act on the retrieved relationship data
4. **Compose JSON:API Response** — the framework builds the final relationship document with resource identifier objects, links, and meta

### Parallel Execution

Relationship resolution within the resource pipeline (stage 6) executes concurrently. The framework uses the `Executor` provided during `JsonApi4j` construction:

- By default, a `CachedThreadPool` is used
- You can provide any `Executor`, including `Executors.newVirtualThreadPerTaskExecutor()` for Project Loom support
- For multi-resource responses, batch resolvers (`BatchReadToManyRelationshipOperation`, `BatchReadToOneRelationshipOperation`) are strongly recommended — they resolve relationships for all resources in a single downstream call rather than N sequential calls

### Pipeline Extensibility

The pipeline is designed to be extended without modifying core logic. At four defined points (pre-retrieval, post-retrieval, relationships pre-retrieval, relationships post-retrieval), registered plugins can:

- **Do nothing** — the pipeline continues normally
- **Return a document** — the pipeline finishes immediately with the provided document
- **Mutate the request** — the pipeline continues with a modified request context
- **Mutate the document** — the pipeline continues with a modified document

For details on how plugins hook into the pipeline, see the [Plugin System](/plugins/) documentation.
