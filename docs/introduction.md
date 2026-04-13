---
title: "Introduction"
permalink: /introduction/
---

## Overview

**JsonApi4j** is a modern, lightweight Java framework for building well-structured, scalable, and production-ready RESTful APIs.
It streamlines the API design and development process by enforcing a consistent data format, eliminating repetitive boilerplate, and providing clear extension points for advanced use cases.

Unlike generic REST frameworks, **JsonApi4j** is purpose-built around the [JSON:API specification](https://jsonapi.org), which promotes best practices and addresses common pain points in designing and maintaining mature APIs.

This approach helps **organizations** drastically simplify API governance at scale.

By abstracting the repetitive parts of RESTful design, **JsonApi4j** enables **developers** to focus on business logic instead of API plumbing.

## Why JsonApi4j?

The following features and design principles will help you determine whether **JsonApi4j** fits your use case.

### Organizational & Business Motivation

Modern systems often consist of multiple services that need to expose and consume consistent data structures.
**JsonApi4j** helps achieve this by:

- Implements the [JSON:API specification](https://jsonapi.org), providing a predictable, efficient, and scalable data exchange format - eliminating the need for custom, company-wide API guidelines.
- Generates [OpenAPI specifications](https://swagger.io/specification/) out of the box, enabling clear and transparent API documentation across the organization.

### Engineering Motivation

Whether you're standardizing your organization's API layer or building a new service from scratch, **JsonApi4j** provides a strong foundation for creating robust, performant, and secure APIs.

- **Framework Agnostic.** Works with modern Java web frameworks such [Spring Boot](https://spring.io/projects/spring-boot) and [Quarkus](https://quarkus.io/).
  The HTTP layer is built on top of the [Jakarta Servlet API](https://jakarta.ee/specifications/servlet/), the foundation for all Java web applications.

- **JSON:API-compliant request and response processing.** Includes automatic error handling fully aligned with the JSON:API specification.

- **Pluggable architecture.** The Plugin System provides an extension mechanism for **JsonApi4j** that allows developers to hook into the request processing pipeline and enrich JSON:API behavior without modifying core logic. Out from the box provides two plugins - Access Control plugin and OpenAPI plugin.

- **Flexible authentication and authorization model.** Supports fine-grained access control, including per-field data anonymization based on access tier, user scopes, and resource ownership. Implemented as a separate plugin.

- **Parallel and concurrent execution.** The framework parallelizes every operation that can safely run concurrently - from relationship resolution to compound document processing - and supports advanced concurrency optimizations, including virtual threads.

- **Compound Documents.** Supports multi-level `include` queries (for example, `include=comments.authors.followers`) for complex, client-driven requests.
  The compound document resolver is available as a standalone, embeddable module that can also run at the API Gateway level, using a shared resource cache to reduce latency and improve performance.

- **Sparse Fieldsets**. Supports `fields[TYPE]=field1,field2` to return only requested attributes per resource type, reducing payload size and improving response efficiency for client-driven data selection.

- **Declarative approach with minimal boilerplate.** Simply define your domain models (resources and relationships), supported operations, and authorization rules - the framework handles the rest.

## Sample Apps

Example applications are available in the [examples](https://github.com/MoonWorm/jsonapi4j/tree/main/examples) directory — check them out for practical guidance on using the framework.
