---
title: "Performance Tuning"
permalink: /performance/
---

Here are some practical tips for optimizing your **JsonApi4j** application:
* **Implement bulk reads using `filter[id]=x,y,z`**. Always implement bulk resource fetching by ID. If the framework can't find a bulk operation, it will fall back to sequential "read-by-id" calls - which can significantly increase response time.
* **Use batch relationship operations**. Improve relationship resolution performance by implementing `BatchReadToManyRelationshipOperation<...>` or `BatchReadToOneRelationshipOperation<...>`. When available, these are preferred over basic operations. For example, when reading multiple users and resolving their relationships, the framework will issue a single batched request instead of N (one per user).
* **Leverage in-house relationship resolution**. Whenever possible, resolve relationships directly from your existing in-memory resource models to avoid unnecessary downstream requests. This optimization applies to read resource operations when relationship linkages can be derived directly from the internal data model. To enable it, implement `ReadToOneRelationshipOperation#readForResource(...)` or `ReadToManyRelationshipOperation#readForResource(...)` where applicable.
* **Tune the `ExecutorService`**. **JsonApi4j** uses a shared `ExecutorService` for parallel execution. You can configure your own implementation depending on your workload characteristics - for example:
  * `Executors.newCachedThreadPool()` for dynamic scaling
  * `Executors.newFixedThreadPool(10)` for predictable concurrency
  * `Executors.newVirtualThreadPerTaskExecutor()` to experiment with Project Loom virtual threads
* **Adjust JsonApi4j configuration properties**. Some properties can significantly influence performance, especially for Compound Documents:
  * `jsonapi4j.compound-docs.maxHops=1` - limits relationship nesting depth to one level
  * `jsonapi4j.compound-docs.maxIncludedResources=100` - caps the total number of included resources resolved per request
  * Otherwise, granting overly broad access can generate an unsustainable load on the backend system.

Fine-tuning these areas can help you balance performance, resource usage, and response time according to your system's scale and complexity.
