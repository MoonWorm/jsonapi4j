# JsonApi4j Documentation

## Introduction

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

- 🧩 Implements the [JSON:API specification](https://jsonapi.org), providing a predictable, efficient, and scalable data exchange format — eliminating the need for custom, company-wide API guidelines.
- 📘 Generates [OpenAPI specifications](https://swagger.io/specification/) out of the box, enabling clear and transparent API documentation across the organization.

### Engineering Motivation

Whether you’re standardizing your organization’s API layer or building a new service from scratch, **JsonApi4j** provides a strong foundation for creating robust, performant, and secure APIs.

- ⚙️ **Framework Agnostic.** Works with all modern Java web frameworks — including [Spring Boot](https://spring.io/projects/spring-boot), [Quarkus](https://quarkus.io/), and [JAX-RS](https://www.oracle.com/technical-resources/articles/java/jax-rs.html).  
  The HTTP layer is built on top of the [Jakarta Servlet API](https://jakarta.ee/specifications/servlet/), the foundation for all Java web applications.

- 🔄 **JSON:API-compliant request and response processing.** Includes automatic error handling fully aligned with the JSON:API specification.

- 🔐 **Flexible authentication and authorization model.** Supports fine-grained access control, including per-field data anonymization based on access tier, user scopes, and resource ownership.

- 🚀 **Parallel and concurrent execution.** The framework parallelizes every operation that can safely run concurrently — from relationship resolution to compound document processing — and supports advanced concurrency optimizations, including virtual threads.

- 📦 **Compound Documents.** Supports multi-level `include` queries (for example, `include=comments.authors.followers`) for complex, client-driven requests.  
  The compound document resolver is available as a standalone, embeddable module that can also run at the API Gateway level, using a shared resource cache to reduce latency and improve performance.

- 🔌 **Pluggable architecture.** Designed for extensibility with rich customization capabilities and support for user-defined plugins.

- 🧠 **Declarative approach with minimal boilerplate.** Simply define your domain models (resources and relationships), supported operations, and authorization rules — the framework handles the rest.

## Sample apps

## Starting guide

## Framework internals

### Project structure

- 🔧 Modular & Embeddable — use parts independently depending on the context:
  - 🌀 [jsonapi4j-core](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-core) — a lightweight JSON:API request processor ideal for embedding into non-web services, f.e. CLI tools that need to handle JSON:API input/output but without a need to carry all HTTP dependencies and specifics.
  - 🔌 [jsonapi4j-rest](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest) — Servlet API HTTP base for integration with other popular Web Frameworks. Can also be used for a plain Servlet API web application.
  - 🌱 [jsonapi4j-rest-springboot](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-rest-springboot) — [Spring Boot](https://spring.io/projects/spring-boot) auto configurable integration.
  - 🌐 [jsonapi4j-compound-docs-resolver](https://github.com/MoonWorm/jsonapi4j/tree/main/jsonapi4j-compound-docs-resolver) — a standalone compound documents resolver that automatically fetches and populates the `included` section of a JSON:API response — perfect for API Gateway-level use or microservice response composition layers.

### Access Control

#### Evaluation stages

Access control evaluation is executed twice for request lifecycle - for **inbound** and **outbound** stage.

![Access Control Evaluation Stages](access-control-evaluation-stages-medium.png)

During the **inbound** stage JsonApi4j application just received a request, but hasn't triggered data fetching from a downstream data source. Access control rules are evaluated for `JsonApiRequest` since there no other data available yet.
If access control requirements are not met there will be no any further data fetching stages and **data** field will be fully anonymized.

**Outbound** stage is executed after gathering data from a data source, composing response document, and right before sending it to the client. Access control rules are evaluated for each resource/resource identifier withing a generated JSON:API Document. Resource documents usually contain full [JSON:API Resource Objects](https://jsonapi.org/format/#document-resource-objects) while Relationship documents consist of [Resource Identifier Objects](https://jsonapi.org/format/#document-resource-identifier-objects) only.
In case of **Resource Documents** access control requirements can be set for either:
- Entire JSON:API Resource. If access control requirements are not met - entire resource will be anonymized.
- Any member of the JSON:API Resource (e.g. 'attributes', 'meta'). If access control requirements are not met - only this particular field will be anonymized.
- Entire 'attributes' member of the JSON:API Resource. If access control requirements are not met - entire 'attributes' section will be anonymized.
- Any member of the 'attributes' abject. If access control requirements are not met - only this particular field will be anonymized.
- Any relationship. If access control requirements are not met for the relationship - relationship data fetching process will not be triggered and the relationship data will be anonymized.

In case of **Relationship Documents** access control requirements can be set for either:
- Entire JSON:API Resource Identifier object. If access control requirements are not met - entire resource identifier will be anonymized.
- Any member of the JSON:API Resource Identifier (e.g. 'meta'). If access control requirements are not met - only this particular field will be anonymized.

By default, JsonApi4j allows everything (no Access Control evaluations), but it's always possible to enforce rules for either both or just one of these stage.

#### Access Control Requirements

There are four requirements that can be assigned in any combination:
- **Authentication requirement** - checks if request is sent on behalf of authenticated client/user. Can be used to restrict anonymous access.
- **Access tier requirement** - checks whether the client/user that originated the request belongs to a particular group e.g. 'Admin', 'Internal API consumers', 'Public API consumers'. This helps to organize access to your APIs based on so-called tiers.
- **OAuth2 Scope(s) requirement** - checks if request was authorised to access user data protected by a certain OAuth2 scope(s). Usually, this information is carried within JWT Access Token.
- **Ownership requirement** - checks if requested resource belongs to a client/user that triggered this request. This is used for those APIs where user can view only its own data, but not others data.

If any of specified requirements are not met - the marked section or the entire object will be anonymized.

#### Setting Principal Context

By default, the framework uses `DefaultPrincipalResolver` which relies on the next HTTP headers in order to resolve the current auth context:

1. `X-Authenticated-User-Id` - to check if request is sent on behalf of authenticated client/user, considers as true if not null/blank. Is also used for ownership checks.
2. `X-Authenticated-Client-Access-Tier` - for principal's Access Tier. By default, supports the next values: 'NO_ACCESS', 'PUBLIC', 'PARTNER', 'ADMIN', 'ROOT_ADMIN'. It's possible to declare your own tiers by implementing `AccessTierRegistry`.
3. `X-Authenticated-User-Granted-Scopes` - for getting OAuth2 Scopes which user has granted the client, space-separated string

It is also possible to implement your own `PrincipalResolver` that tells the framework how to retrieve Principal-related info from an incoming HTTP request.

Later, the framework will use this info for Inbound/Outbound evaluations.

#### Setting Access Requirements

How and where to declare your Access Control requirements?

There are two main approaches:
1. Via Java annotations. If you are working with **jsonapi4j-core** it's possible to place Access Control annotations on either a custom `ResourceObject`, or a custom `Attributes` object. Annotations can be placed both on class and field levels. If you're working with modules that operates higher abstractions - **jsonapi4j-rest** or **jsonapi4j-rest-springboot** - you can place annotations only for an Attributes Object. Here is the list of annotations that can be used: `@AccessControlAuthenticated`, `@AccessControlScopes`, `@AccessControlAccessTier`, `@AccessControlOwnership`. This approach is preferable for setting Access Control requirements for Attributes.
2. Via **JsonApi4j** plugin system. You can use `OperationInboundAccessControlPlugin` plugin for your Operations - that will be used for the Inbound Access Control evaluations. `ResourceOutboundAccessControlPlugin` can be used for the `Resource` implementations and be applied for JSON:API Resource Objects during the Outbound Access Control evaluations. `RelationshipsOutboundAccessControlPlugin` can be used for the `Relationship` implementations and be applied for JSON:API Resource Identifier Objects during the Outbound Access Control evaluations. This is approach is preferable for all other cases.

If the system detects a mix of settings it merges them giving priority to ones that were set programmatically via Plugins.

#### Examples

Example 1: Outbound Access Control

Let's hide user's credit card number for everyone but the owner. By achieving that `@AccessControlOwnership(ownerIdFieldPath = "id")` must be placed on top of `creditCardNumber` field.
We can also put `@AccessControlAuthenticated` to ensure the user is authenticated and `@AccessControlScopes(requiredScopes = {"users.sensitive.read"})` if we want to protect access to this field by checking whether the client has gotten a user grant for this data.

```java
public class UserAttributes {
    
    private final String firstName;
    private final String lastName;
    private final String email;
    
    @AccessControlAuthenticated
    @AccessControlScopes(requiredScopes = {"users.sensitive.read"})
    @AccessControlOwnership(ownerIdFieldPath = "id")
    private final String creditCardNumber;
    
    // constructors, getters and setters

}
```

Example 2: Inbound Access Control

Let's only allow a new user creation for the admin clients.

```java
@Component
public class CreateUserOperation implements CreateResourcesOperation<UserDbEntity> {

    // methods implementations

    @Override
    public List<OperationPlugin<?>> plugins() {
      return List.of(
        OperationInboundAccessControlPlugin.builder()
          .requestAccessControl(
            AccessControlRequirements.builder()
              .requiredAccessTier(
                AccessControlAccessTierModel.builder()
                  .requiredAccessTier(TierAdmin.ADMIN_ACCESS_TIER)
                  .build()
              )
              .build()
          )
          .build()
      );
    }

}
```

### Request Validation

Examples of how to add custom validation logic

### OpenAPI Specification

Since JSON:API has predetermined list of operations and schemas Open API Spec generation can be fully automated.

JsonApi4j can generate an instance of `io.swagger.v3.oas.models.OpenApi` model and then expose it either through a Maven Exec Plugin or via dedicated endpoint.

Here is two ways of how to generate an Open API Specification for you APIs:

1. Access via HTTP endpoint. By default, you can access either JSON or YAML version of the Open API Specification by accessing [/jsonapi/oas](http://localhost:8080/jsonapi/oas) endpoint. It supports 'format' query parameter that can be either 'json' or 'yaml'. Always fallbacks to JSON format.
2. Via Maven Exec Plugin. TBD

By default, JsonApi4j generate all schemas and operations for you. But if you need to enrich it with more data e.g. 'info', 'components' -> 'securitySchemes' or custom HTTP headers you need to explicitly configure that in `JsonApi4jProperties` ('oas' section) via `application.yaml` if you're using 'jsonapi4j-rest-springboot' or via proper `JsonApi4jServletContainerInitializer` bootstrapping if you're relying on Servlet API only from 'jsonapi4j-rest'.

### Compound documents

[Compound Documents](https://jsonapi.org/format/#document-compound-documents) is a part of JSON:API specification that describes the way to include related resources in one request. For example, if you want to request some 'users' you can also ask the server to include related resources to these users. It's worth mentioning that you can only ask for those resources that enabled via relationships. All resolved resourced are placed as a flat structure into a top-level "included" field. In order to request related resources "include" query parameter must be used, for example `/users?page[cursor]=xxx&include=citizenships`.

It is allowed to request multiple relationships in one go - just specify relationship names using comma ',' as a separator, for example `include=citizenships,placeOfBirth`

Compound Documents feature also supports multi-level relationship resolution. That means that client can request a chain of relationships, f.e. `include=placeOfBirth.currency`. The relationships sequence is a dot-separated string that must be a valid chain of relationships - meaning they must exist for the resources on each stage. This particular example would trigger the process that resolves related resources in two stages - firstly, JsonApi4j will resolve 'placeOfBirth' relationship which is represented by Country resource. Then, as a second stage, the framework will resolve 'currency' of the previously resolved countries. 'currency' relationship must exist for Country resource.

Since every level generates a new wave of requests it's important to remember that and use these feature carefully. JsonApi4j relies on batch operations (e.g. `filter[id]=1,2,3,4,5`) that's why it's important to implement this operation for all resources that can be requested as someone's relationship. If the operation is not implemented the framework tries to fallback on sequential 'read by id' operation if it exists.

Let's define what does resolution stage means in terms of how framework resolves Compound Documents. For example, `include=citizenships,placeOfBirth.currency` would be parsed into two stages - first stage includes 'citizenships' and 'placeOfBirth' relationships. The second stage includes 'currency' relationship. Within each stage the framework groups all related resources by their types and associated list of identifiers and sends as many parallel request as many resource types were detected.

In order to be able to control the amount of these extra requests the framework provides some settings and guardrails to control the limits. Refer `CompoundDocsProperties` for more details, for example `maxHops` settings allows to define how many levels your system supposed to support.

Compound Documents resolver is part of a dedicated module 'jsonapi4j-compound-docs-resolver'. By default, this feature is disabled on the application server, but it can be enabled by setting `enabled` property to `true`. Since the logic is part of a separate independent module it opens multiple options where to host this logic. There are at least two the most obvious options: on the same application server or on the API Gateway level.

- Compound docs works as a post processor. First main request is executed.
- Sequence diagram - stages
- Point the difference in 'includes' for Primary Resources and Relationship requests (how relationship request refers self).
- CacheControlPropagator examples, how to configure an external Cache that relies on HTTP Cache Control headers

### Register custom error handlers
- Example of how to declare a custom error handler

### Performance tunings

- batch read relationship operations
- custom executor service, 
- jsonApi4j properties, e.g. maxHops

## JSON:API Specification deviations

1. JsonApi4j encourages flat resource structure e.g. '/users' and '/articles' instead of '/users/{userId}/articles'. This approach fully automates default 'links' generation and enables the gates for automatic Compound Documents resolution.
2. No support for [Sparse Fieldsets](https://jsonapi.org/format/#fetching-sparse-fieldsets) (maybe later)
3. No support for [client generated ids](https://jsonapi.org/format/#document-resource-object-identification) ('lid') -> use 'id' field and set client-generated id there.
4. JSON:API spec is agnostic about the pagination strategy (e.g. 'page[number]' and 'page[size]' for limit-offset), while the framework encourages Cursor pagination ('page[cursor]')
5. Doesn't support JSON:API Profiles and Extensions (maybe later)
6. Default relationships concept, no 'relationships'->'{relName}'->'data' resolution by default. This is done to have more control under extra +N requests per each existing relationship
7. The framework enforces the requirement for implementing either 'Filter By Id' ('/users?filter[id]=123') operation or 'Read By Id' ('/users/123') operation because Compound Docs Resolver uses them to compose 'included' section.
