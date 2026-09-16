---
title: "OpenAPI Plugin"
permalink: /openapi-plugin/
---

The OpenAPI Specification Plugin (OAS) builds on top of the JsonApi4j plugin system to provide automatic, always-in-sync API documentation.
It observes registered resources, relationships, and operations and translates them into an OpenAPI-compliant model.
Because the specification is derived directly from the same metadata used at runtime, it accurately reflects the actual behavior of your JSON:API endpoints without requiring manual maintenance.

To enable the plugin, add the following dependency:

```xml
<dependency>
  <groupId>pro.api4</groupId>
  <artifactId>jsonapi4j-oas-plugin</artifactId>
  <version>${jsonapi4j.version}</version>
</dependency>
```

If you're using Spring Boot or Quarkus, the plugin is auto-configured with default values.

### Accessing the Specification

**JsonApi4j** generates an `io.swagger.v3.oas.models.OpenAPI` model and exposes it through a dedicated endpoint.

By default, the specification is available at `/jsonapi/oas`. It supports an optional `format` query parameter (`json` or `yaml`) — defaulting to `json` if not provided.

The document the sample app produces is published as a [live OpenAPI example](/oas-example/) — regenerated on
every build and asserted by the test suite, so it is what the plugin emits today rather than what it emitted once.

Out of the box, **JsonApi4j** generates all schemas and operations automatically. JSON:API parameters, request/response schemas, and error models are all included.

### Enriching the Specification

To add metadata beyond what the framework generates automatically (e.g., `info`, `securitySchemes`), you have two options:

**Via configuration properties** — set OpenAPI metadata in `application.yaml` / `application.properties`. See the [Spring Boot sample config](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml#L21) or [Quarkus sample config](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties#L12) for reference.

**Via annotations** — place these on your domain and operation classes for fine-grained control:

| Annotation | Placement | Purpose |
|-----------|-----------|---------|
| `@OasResourceInfo` | On `Resource` class | Declares the attributes class, the singular resource name, and how the resource's identifier is described and exemplified |
| `@OasRelationshipInfo` | On `ToOneRelationship` or `ToManyRelationship` class | Declares the resource types the relationship links to, and the type of the `meta` its resource linkage carries |
| `@OasOperationInfo` | On operation class or individual operation methods | Overrides the generated operation summary, description and request body type, and declares sortable fields, filters, pagination styles, application-specific parameters and OAuth2 security requirements |

Example:

```java
@JsonApiResource(resourceType = "users")
@OasResourceInfo(
        resourceNameSingle = "user",
        attributes = UserAttributes.class,
        resourceIdDescription = "User unique identifier",
        resourceIdExample = "3"
)
public class UserResource implements Resource<UserDbEntity> {
    // ...
}
```

`resourceIdDescription` and `resourceIdExample` are declared once and reach every place that resource's identifier
appears — the `{id}` path parameter of each operation acting on one instance, and the `id` member of the resource
schema. They belong to the resource rather than to an operation because every operation asks for the same
identifier; declaring them per operation is what lets one endpoint carry an example and the next one not. Request
bodies keep their own `id` wording, which says whether the id must match the path or may be omitted, since that is
about the operation.

```java
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    @OasOperationInfo(summary = "List all users", description = "Returns a paginated list of users")
    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        // ...
    }
}
```

### Using springdoc Instead of the Built-in Endpoint

If your Spring Boot app already serves an OpenAPI document through springdoc, feed it the JsonApi4j customizers
rather than running both endpoints. springdoc's `OpenApiCustomizer` declares the same `customise(OpenAPI)` method
the framework's customizers do, so one bean is the whole integration:

```java
@Bean
public OpenApiCustomizer jsonApi4jOpenApiCustomizer(JsonApi4j jsonApi4j) {
    return openApi -> OasDocument.customizers(jsonApi4j).forEach(customizer -> customizer.customise(openApi));
}
```

`OasDocument.customizers(...)` returns them in the order they must be applied, so the document springdoc publishes
matches the one the built-in endpoint would. Everything each customizer needs — the registries, the root
configuration, the OAS configuration — comes off the `JsonApi4j` bean.

To build a document without springdoc, `OasDocument.generate(jsonApi4j)` returns a finished `OpenAPI`.

See the [Spring Boot sample app](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/java/pro/api4/jsonapi4j/sampleapp/config/swagger/SpringJsonApi4jSpringDocConfig.java)
for the complete configuration.

### Request Bodies

Every write operation gets a request body derived from what JSON:API prescribes for it — nothing to declare:

| Operation | Request body schema |
|-----------|---------------------|
| `POST /{type}` | `<Type>CreateRequestDoc` — `id` optional, so the client may generate it |
| `PATCH /{type}/{id}` | `<Type>UpdateRequestDoc` — `id` required |
| `PATCH /{type}/{id}/relationships/{name}` (to-one) | `ToOneRelationshipRequestDoc` — linkage, nullable to clear |
| `POST`/`PATCH`/`DELETE` `/{type}/{id}/relationships/{name}` (to-many) | `ToManyRelationshipsRequestDoc` — array of linkage |
| `DELETE /{type}/{id}` | none |

`<Type>` is the resource type capitalized, so `users` yields `UsersCreateRequestDoc`. The relationship bodies carry no
`<Type>` prefix because they are pure resource linkage — `{"data": {"id": …, "type": …}}` — which looks the same
whatever resource it points at, so one schema serves them all. The exception is a relationship declaring
`@OasRelationshipInfo(resourceLinkageMetaType = …)`: its linkage carries a typed `meta`, which makes the body specific
to that relationship, so it gets `<Type><Relationship>ToOneRelationshipRequestDoc` /
`<Type><Relationship>ToManyRelationshipsRequestDoc` instead. The same typed identifier is reused in responses, so a
client sees one shape for the linkage whichever direction it travels.

Every resource schema pins its `type` to the single value JSON:API allows for it — `UsersResource.type` is
`enum: ["users"]`, not an open string. That is the same rule the framework enforces at runtime by answering a
mismatched request body with a `409`, so the document and the server now say the same thing. It also makes `included`
resolvable: the union of resource schemas a document may carry declares a `discriminator` on `type`, mapping each
resource type to the schema describing it, so a generated client deserializes each member into its concrete class
instead of handing the caller a union to switch on.

These are separate from the response documents on purpose: a request carries no `links` or `included`, and `id` is
optional on create but mandatory on update. To document a body the framework cannot derive, set
`@OasOperationInfo(payloadType = YourPayload.class)` — the declared type replaces the derived one and its schema is
registered automatically.

### Required Attributes Are a Create-Only Promise

`@Schema(requiredMode = REQUIRED)` on an attribute says what a **create** must carry, and that is the one place it is
published — `<Type>CreateAttributes`. The other two forms require nothing:

| Schema | Referenced by | `required` |
|---|---|---|
| `<Type>CreateAttributes` | `POST /{type}` | as the resource declared it |
| `<Type>UpdateAttributes` | `PATCH /{type}/{id}` | nothing — `PATCH` is a partial update, so a client sends only what it is changing |
| `<Type>Attributes` | every response | nothing — see below |

That is not a weakening — it is the only honest reading. A response cannot promise an attribute is present:

- an attribute with no value is not serialized at all — true of every application, with no plugins involved;
- sparse fieldsets let a client ask for a subset (`?fields[users]=fullName`);
- access control withholds what the caller may not see.

Each of those produces a response that a `required` list copied from the attributes class would reject. A generated
client would model the attribute as non-nullable and fail to read a response the server is entitled to send, and a
strict response validator would reject it — which is exactly what used to happen. The same reasoning is what separates
create from update: requiring an attribute on `PATCH` would reject a partial update the framework accepts.

Each reason is described by whoever owns it, so a document never mentions a plugin the application does not run. The
framework's own pass states the first line only; the sparse fieldsets customizer adds *"A client can narrow what is
returned with `fields[users]`"*, and access control adds what it withholds. Turn both plugins off and the attributes
schema says just the one sentence.

Each form is published only for the operation that uses it: a read-only resource gets none of the write forms, and a
resource with only an update gets only `<Type>UpdateAttributes`. On a read-only resource the annotation therefore
publishes nothing at all.

One thing `required` is *not*: enforced. The framework does not reject a create for a missing attribute on the strength
of `requiredMode` — that is what `validateCreate` is for. So the annotation is a claim about your own operation, and it
is worth keeping the two in step: the sample app declares `fullName` and `email` required and its `validateCreate`
checks both. A `required` the validator does not enforce is a document that promises something the server does not do.

Where the Access Control plugin guards an individual attribute, the response schema also says so on that attribute —
*"Absent from the response unless the caller qualifies — requires the scopes `users.sensitive.read`"* — using the
requirement's own `description()` where one was given. Absence alone tells a client nothing; naming the requirement
turns it into something they can act on.

### Operation Ids

Every operation gets an `operationId`, derived from what it does and what it acts on:

```
get-all-users                        create-single-user
get-single-user                      update-single-user
get-user-relatives-relationship      delete-user-relatives-relationship
```

Client generators name their methods after it, so it is part of your published contract — treat a rename as a
breaking change.

Your `resourceType` stays exactly as you declared it, plural as JSON:API asks: it is what paths, tags, schema names
and the `type` member are built from, and `get-all-users` uses it as-is. A singular form appears only where the plural
would read wrong — in the ids of operations acting on one resource (`get-single-user`) and in the generated summaries
and descriptions ("Retrieves user details by resource id."). The plugin guesses it from the resource type
(`countries` → `country`, `addresses` → `address`, `status` → `status`, left alone when unrecognised). Set
`@OasResourceInfo(resourceNameSingle = …)` on the resource whenever that guess reads wrong. There is no override for
the plural — that is the resource type itself.

### Query Parameters

The framework parses `sort`, `page[…]` and `fields[…]` for every request, but only the operation can act on them —
so the document publishes what the operation declares, never what the request layer merely accepts.

| Parameter | Published when |
|-----------|----------------|
| `include` | the resource has relationships with a read operation |
| `fields[TYPE]` | the sparse fieldsets plugin is enabled and the operation returns resources |
| `page[cursor]` | the operation is paginated — the default |
| `page[limit]`, `page[offset]` | `@OasOperationInfo(pagination = {CURSOR, LIMIT_OFFSET})` |
| `sort` | `@OasOperationInfo(sortableFields = {"fullName", "email"})` |
| `filter[…]` | `@OasOperationInfo(filters = {@Filter(name = "region")})` |

```java
@OasOperationInfo(
        sortableFields = {"fullName", "email"},
        pagination = {PaginationStyle.CURSOR, PaginationStyle.LIMIT_OFFSET},
        filters = {
                @Filter(name = "id", example = "3"),
                @Filter(name = "region", description = "Filter by region", example = "Asia")
        }
)
public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) { … }
```

`sortableFields` becomes the parameter's allowed values in both directions — `fullName`, `-fullName`, `email`,
`-email`. Pagination defaults to cursor alone, so an operation that does not read `page[limit]` never advertises it.

A filter is declared by its dimension, not by its parameter name: the framework spells it `filter[id]`, makes it
optional and multi-valued as JSON:API defines filters, and bounds it with `validation.maxElementsInFilterParam`.

Every multi-valued parameter is published as `style: form, explode: false`, so a generated client sends
`?include=citizenships,placeOfBirth` — the form JSON:API asks for. Saying nothing would not be neutral: OpenAPI's
default for a query parameter is a repeated key (`?include=a&include=b`). The request layer accepts either form, so
this is about what the document promises rather than what the server takes.

### Application-specific Parameters

`@OasOperationInfo(parameters = …)` documents parameters the framework cannot derive — the application's own query
parameters, which reach an operation through `JsonApiRequest#getCustomQueryParams()`.

```java
@OasOperationInfo(
        parameters = @Parameter(name = "tenant", description = "Tenant the request is scoped to", required = false)
)
```

Naming a parameter the framework already generates — `id`, `include`, `sort`, `page[…]`, `fields[…]`, `filter[…]` —
contributes a description and example to it and nothing else, so the generated schema keeps the constraints it took
from the running configuration. For the id, prefer `@OasResourceInfo`: it reaches every operation at once.

One `fields[TYPE]` parameter is published per type the document can carry: the primary resource plus what it can
include. That is one level — a client may nest includes (`include=citizenships.currencies`) and select fields on what
comes back, but the document describes the immediate contract and leaves the rest of the graph to be discovered from
the related resource's own operations. The `included` schema describes the same single level, so the two never
disagree.

Limits configured under `jsonapi4j.validation` are projected into the schemas: `page[limit]` carries its `maximum`,
`sort` and `include` their `maxItems`, and the `id` path parameter its `maxLength`.

### Deprecating an Operation

`deprecated` marks an operation on its way out, and tooling picks it up — Swagger UI strikes it through, generators
emit a deprecation on the method they create:

```java
@OasOperationInfo(deprecated = true)
public UserDbEntity readById(JsonApiRequest request) { … }
```

A whole resource can go at once, which marks every operation acting on it:

```java
@OasResourceInfo(deprecated = true)
public class LegacyUserResource implements Resource<UserDbEntity> { … }
```

Deprecation only ever widens: a resource takes its operations with it, an operation may retire on its own, and
nothing opts back in. Java's `@Deprecated` is not read for this — use the attribute.

### Responses

Every operation documents the status it answers with, including the writes that return no body:

| Operation | Success response |
|-----------|------------------|
| `GET` (resource, collection, relationship) | `200` with the corresponding document |
| `POST /{type}` | `201` with `<Type>SingleResourceDoc` |
| `PATCH` / `DELETE`, and every relationship write | `204`, no body |

Collection and to-many responses name their pagination `links` and `meta` members rather than leaving them an untyped
map, so a client can page from the document alone — see [Pagination](/pagination/) for what each one means.

The error responses an operation documents are derived from what the framework can actually answer with, so a
generated client handles every failure it may see and none that it cannot:

| Status | Documented on |
|--------|---------------|
| `400`, `405`, `406`, `429`, `500` | every operation |
| `401` | operations declaring an OAuth2 security requirement — the host's security layer rejects the request before the framework sees it |
| `403` | `POST /{type}` (JSON:API reserves id generation to the server), and any write that declares an `@AccessControl` requirement — a write the plugin does not guard has nothing to refuse |
| `404` | operations whose path carries an `{id}` — a collection read and a create have nothing that can be missing |
| `409` | `POST /{type}` and `PATCH /{type}/{id}`, whose body repeats a type or id the collection or path already fixed |
| `415` | operations carrying a request body |

Error responses carry a ready example per status code, embedded as JSON so that spec linters and mock servers read
them as documents rather than as strings.

### Shared Components

Responses and parameters that every operation repeats verbatim are published once under `components` and referenced
from there:

```json
"components": {
  "responses": {
    "TooManyRequests": { "description": "Too many requests. …", "content": { … }, "headers": { … } }
  }
},
"paths": {
  "/jsonapi/users": {
    "get": { "responses": { "429": { "$ref": "#/components/responses/TooManyRequests" } } }
  }
}
```

Nothing the document says changes — a `$ref` and the object it points at are the same thing to any reader. On the
sample app it halves the file.

A response or parameter is shared only when it appears more than once *and* looks the same everywhere; one that
varies is left inline at every occurrence, which is why `include` stays put — it names each resource's own
relationships. Names come from what the status code means (`TooManyRequests`, `NoContent`) or from the parameter
with the brackets OpenAPI forbids in a component key removed (`fields[users]` → `FieldsUsers`).

Sharing runs after any customizer the application registered, so a response tuned by hand is shared in its final
shape.

### What Other Plugins Contribute

Two customizers ship with this plugin and document what a *neighbouring* plugin makes reachable. Each applies only
when its plugin is registered and enabled, so a disabled plugin contributes no documentation just as it contributes
no behaviour. Spring Boot and Quarkus need no wiring for either.

**Sparse Fieldsets** adds one `fields[TYPE]` parameter per type an operation's response may carry.

**Access Control** turns what it enforces into what the document says:

| Declared | Published as |
|---|---|
| `@AccessControl(scopes = …)` | the operation's `security`, one entry per alternative — `(A or B) and (C or D)` becomes four |
| `@AccessControl(authenticated = ANONYMOUS)` | `security: []`, OpenAPI's way of saying an operation needs no authentication |
| any requirement, on a write | `403` — reads never carry one, since a denied read answers with an empty document and `200` |
| a requirement's `description` | appended to the operation's, including entitlements and policies, which OpenAPI cannot state |

Access control supplies the scopes; the OAS configuration supplies the flows they hang off. A requirement declared
there wins over `@OasOperationInfo(securityConfig = …)`. Each scope is published only on the flows whose own
`scopes` declare it — OpenAPI reads an operation's scopes against the scheme naming them, so a flow that cannot
grant one is left out rather than made to reference it. A scope no flow declares at all fails generation.

### Diagnostics

`jsonapi4j.oas.diagnostics` decides what a document the plugin cannot vouch for costs you, in increasing
order of strictness:

| Mode | Document is built | A loose end |
|---|---|---|
| `DISABLED` | on first request | ignored — the checks that exist only to report are skipped, not just silenced |
| `WARN` *(default)* | on first request | logged; the document is published anyway |
| `FAIL_ON_REQUEST` | on first request | the endpoint answers `500` with a JSON:API error document |
| `FAIL_ON_STARTUP` | during startup | the application does not start |

`FAIL_ON_STARTUP` is the only mode that builds the document eagerly, and that is the point of it: every
other mode discovers a broken document on whichever request happens to ask for it first, which in a
deployed application means discovering it in production. It costs the generation work at every boot —
a few hundred milliseconds on a small API — so an application that cannot afford that but still must
not publish a document it cannot vouch for wants `FAIL_ON_REQUEST` instead.

This setting governs **reports** only. A document that would say something *untrue* is rejected
whatever the mode — two schemas claiming one name would publish one of them under the other's shape,
and no setting should let that reach a client. `FAIL_ON_STARTUP` is therefore also the only mode under
which such a rejection cannot first surface on a live endpoint.

### Customizing the Generated Document

Anything the generator produces can be tuned by registering an `OasCustomizer`. Customizers run after the built-in
ones, in the order registered, so each one sees the finished document:

```java
public class RateLimitHeadersCustomizer implements OasCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .map(operation -> operation.getResponses().get("429"))
                .filter(Objects::nonNull)
                .forEach(response -> response.addHeaderObject("X-RateLimit-Remaining", new Header()
                        .required(true)
                        .description("Number of tokens currently remaining.")
                        .schema(new IntegerSchema())
                        .example(5)));
    }

}
```

Register it as a bean and the plugin picks it up — `@Bean` in Spring Boot, `@Produces` in Quarkus. With the plain
Servlet API, pass them to the plugin directly:

```java
new JsonApiOasPlugin(oasProperties, List.of(new RateLimitHeadersCustomizer()));
```

This is the extension point for anything the framework cannot derive: response headers your gateway adds, status
codes only your domain produces, descriptions you want worded differently. See the
[sample apps](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-sampleapp-domain/src/main/java/pro/api4/jsonapi4j/sampleapp/oas/RateLimitHeadersCustomizer.java)
for a working example.

### Available Properties

| Property name                               | Default value | Description                                                                                                             |
|---------------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| `jsonapi4j.oas.enabled` | `true` | Enables/disables OAS plugin and OAS endpoint exposure. |
| `jsonapi4j.oas.diagnostics` | `WARN` | What happens when the generated document has a loose end — a `$ref` resolving to nothing, or a requirement it could not state. `DISABLED` reports nothing and skips the checks that exist only to report; `WARN` logs and publishes anyway; `FAIL_ON_REQUEST` refuses to serve it; `FAIL_ON_STARTUP` builds the document during startup and refuses to start. See [Diagnostics](#diagnostics). |
| `jsonapi4j.oas.oasRootPath` | `/jsonapi/oas` | Root path for generated OpenAPI spec endpoint. Honoured identically by all three integrations; it must not be the same path as `jsonapi4j.rootPath`, which is checked at startup. |
| `jsonapi4j.oas.info.title` | `JsonApi4j API Sample Title` | OpenAPI info.title. |
| `jsonapi4j.oas.info.description` | not set | OpenAPI info.description. |
| `jsonapi4j.oas.info.version` | `1.0.0` | OpenAPI info.version. |
| `jsonapi4j.oas.info.termsOfService` | not set | OpenAPI info.termsOfService URL. |
| `jsonapi4j.oas.info.contact.name` | not set | OpenAPI info.contact.name. |
| `jsonapi4j.oas.info.contact.url` | not set | OpenAPI info.contact.url. |
| `jsonapi4j.oas.info.contact.email` | not set | OpenAPI info.contact.email. |
| `jsonapi4j.oas.info.license.name` | not set | OpenAPI info.license.name. |
| `jsonapi4j.oas.info.license.url` | not set | OpenAPI info.license.url. |
| `jsonapi4j.oas.info.license.identifier` | not set | OpenAPI info.license.identifier (SPDX). |
| `jsonapi4j.oas.externalDocumentation.url` | not set | OpenAPI external docs URL. |
| `jsonapi4j.oas.externalDocumentation.description` | not set | OpenAPI external docs description. |
| `jsonapi4j.oas.oauth2.clientCredentials.name` | not set | OAuth2 client credentials scheme name. Also the name operations reference in their `security` requirements — leave it unset and no operation requires this flow. |
| `jsonapi4j.oas.oauth2.clientCredentials.description` | not set | OAuth2 client credentials description. |
| `jsonapi4j.oas.oauth2.clientCredentials.tokenUrl` | not set | OAuth2 client credentials token URL. |
| `jsonapi4j.oas.oauth2.clientCredentials.scopes[*].name` | not set | OAuth2 scope name. Only scopes listed here can appear in this flow's security requirements. |
| `jsonapi4j.oas.oauth2.clientCredentials.scopes[*].description` | not set | OAuth2 scope description. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.name` | not set | OAuth2 authorization code + PKCE scheme name. Also the name operations reference in their `security` requirements — leave it unset and no operation requires this flow. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.description` | not set | OAuth2 authorization code + PKCE description. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.tokenUrl` | not set | OAuth2 authorization code + PKCE token URL. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.authorizationUrl` | not set | OAuth2 authorization URL (PKCE flow). |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.scopes[*].name` | not set | OAuth2 scope name. Only scopes listed here can appear in this flow's security requirements. |
| `jsonapi4j.oas.oauth2.authorizationCodeWithPkce.scopes[*].description` | not set | OAuth2 scope description. |
| `jsonapi4j.oas.servers[*].name` | not set | OpenAPI server display name. |
| `jsonapi4j.oas.servers[*].url` | not set | OpenAPI server URL. |
| `jsonapi4j.oas.servers[*].enabled` | false | Include server in generated spec or not. |
