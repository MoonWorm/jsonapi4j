---
title: "Configuration"
permalink: /configuration/
---

JsonApi4j is configured differently depending on your web framework. All three integrations support the same set of properties — only the format and loading mechanism differ.

## Configuration by Framework

<div class="tabs" markdown="0">
  <div class="tab-buttons">
    <button class="tab-btn active" data-tab="cfg-springboot">Spring Boot</button>
    <button class="tab-btn" data-tab="cfg-quarkus">Quarkus</button>
    <button class="tab-btn" data-tab="cfg-servlet">Servlet API</button>
  </div>

  <div id="cfg-springboot" class="tab-panel active">
    <p>Spring Boot uses standard <code>application.yaml</code> or <code>application.properties</code> with the <code>jsonapi4j</code> prefix. Properties are bound via <code>@ConfigurationProperties</code>.</p>
    <p><strong>application.yaml:</strong></p>
    <div class="language-yaml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="na">jsonapi4j</span><span class="pi">:</span>
  <span class="na">rootPath</span><span class="pi">:</span> <span class="s">/jsonapi</span>

  <span class="na">meta</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>

  <span class="na">validation</span><span class="pi">:</span>
    <span class="na">maxNumberFilterParams</span><span class="pi">:</span> <span class="m">5</span>
    <span class="na">maxElementsInFilterParam</span><span class="pi">:</span> <span class="m">20</span>
    <span class="na">resourceIdMaxLength</span><span class="pi">:</span> <span class="m">64</span>
    <span class="na">limitMaxValue</span><span class="pi">:</span> <span class="m">100</span>
    <span class="na">maxElementsInIncludeParam</span><span class="pi">:</span> <span class="m">10</span>
    <span class="na">maxElementsInSortByParam</span><span class="pi">:</span> <span class="m">5</span>

  <span class="na">ac</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
    <span class="na">failOnMisconfiguration</span><span class="pi">:</span> <span class="no">false</span>

  <span class="na">sf</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
    <span class="na">requestedFieldsDontExistMode</span><span class="pi">:</span> <span class="s">SPARSE_ALL_FIELDS</span>

  <span class="na">cd</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
    <span class="na">maxHops</span><span class="pi">:</span> <span class="m">3</span>
    <span class="na">maxIncludedResources</span><span class="pi">:</span> <span class="m">100</span>
    <span class="na">mapping</span><span class="pi">:</span>
      <span class="na">users</span><span class="pi">:</span> <span class="s">http://localhost:8080/jsonapi</span>
      <span class="na">countries</span><span class="pi">:</span> <span class="s">http://localhost:8080/jsonapi</span>

  <span class="na">oas</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
    <span class="na">info</span><span class="pi">:</span>
      <span class="na">title</span><span class="pi">:</span> <span class="s">My API</span>
      <span class="na">version</span><span class="pi">:</span> <span class="s">1.0.0</span></code></pre></div></div>
    <p>Spring Boot's standard property resolution applies: <code>application.yaml</code>, <code>application.properties</code>, environment variables, system properties, and profile-specific files all work as expected.</p>
    <p>All JsonApi4j beans are registered with <code>@ConditionalOnMissingBean</code>, so you can override any bean by defining your own in a <code>@Configuration</code> class.</p>
  </div>

  <div id="cfg-quarkus" class="tab-panel">
    <p>Quarkus uses <code>application.properties</code> with the same <code>jsonapi4j</code> prefix. Properties are bound at build time via <code>@ConfigMapping</code>.</p>
    <p><strong>application.properties:</strong></p>
    <div class="language-properties highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="py">jsonapi4j.rootPath</span>=<span class="s">/jsonapi</span>

<span class="py">jsonapi4j.meta.enabled</span>=<span class="s">true</span>

<span class="py">jsonapi4j.validation.maxNumberFilterParams</span>=<span class="s">5</span>
<span class="py">jsonapi4j.validation.maxElementsInFilterParam</span>=<span class="s">20</span>
<span class="py">jsonapi4j.validation.resourceIdMaxLength</span>=<span class="s">64</span>
<span class="py">jsonapi4j.validation.limitMaxValue</span>=<span class="s">100</span>
<span class="py">jsonapi4j.validation.maxElementsInIncludeParam</span>=<span class="s">10</span>
<span class="py">jsonapi4j.validation.maxElementsInSortByParam</span>=<span class="s">5</span>

<span class="py">jsonapi4j.ac.enabled</span>=<span class="s">true</span>
<span class="py">jsonapi4j.ac.failOnMisconfiguration</span>=<span class="s">false</span>

<span class="py">jsonapi4j.sf.enabled</span>=<span class="s">true</span>
<span class="py">jsonapi4j.sf.requestedFieldsDontExistMode</span>=<span class="s">SPARSE_ALL_FIELDS</span>

<span class="py">jsonapi4j.cd.enabled</span>=<span class="s">true</span>
<span class="py">jsonapi4j.cd.maxHops</span>=<span class="s">3</span>
<span class="py">jsonapi4j.cd.maxIncludedResources</span>=<span class="s">100</span>
<span class="py">jsonapi4j.cd.mapping.users</span>=<span class="s">http://localhost:8080/jsonapi</span>
<span class="py">jsonapi4j.cd.mapping.countries</span>=<span class="s">http://localhost:8080/jsonapi</span>

<span class="py">jsonapi4j.oas.enabled</span>=<span class="s">true</span>
<span class="py">jsonapi4j.oas.info.title</span>=<span class="s">My API</span>
<span class="py">jsonapi4j.oas.info.version</span>=<span class="s">1.0.0</span></code></pre></div></div>
    <p>Quarkus uses Smallrye Config for property resolution. List-valued properties accept both comma-separated (<code>jsonapi4j.cd.propagation=FIELDS,CUSTOM_QUERY_PARAMS,HEADERS</code>) and indexed (<code>jsonapi4j.cd.propagation[0]=FIELDS</code>) syntax — both bind to the same list, and the <a href="/meta-api/">Meta API</a> <code>config</code> resource renders either form identically as a JSON array. The same holds for Spring Boot's relaxed binding. All JsonApi4j CDI beans use <code>@DefaultBean</code>, so you can override them with your own <code>@Produces</code> methods.</p>
  </div>

  <div id="cfg-servlet" class="tab-panel">
    <p>For plain Servlet applications, JsonApi4j loads configuration from a YAML or JSON file. No framework-specific property binding is used.</p>
    <p><strong>jsonapi4j.yaml</strong> (on the classpath):</p>
    <div class="language-yaml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="na">rootPath</span><span class="pi">:</span> <span class="s">/jsonapi</span>

<span class="na">meta</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>

<span class="na">validation</span><span class="pi">:</span>
  <span class="na">maxNumberFilterParams</span><span class="pi">:</span> <span class="m">5</span>
  <span class="na">maxElementsInFilterParam</span><span class="pi">:</span> <span class="m">20</span>
  <span class="na">resourceIdMaxLength</span><span class="pi">:</span> <span class="m">64</span>
  <span class="na">limitMaxValue</span><span class="pi">:</span> <span class="m">100</span>
  <span class="na">maxElementsInIncludeParam</span><span class="pi">:</span> <span class="m">10</span>
  <span class="na">maxElementsInSortByParam</span><span class="pi">:</span> <span class="m">5</span>

<span class="na">ac</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
  <span class="na">failOnMisconfiguration</span><span class="pi">:</span> <span class="no">false</span>

<span class="na">sf</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
  <span class="na">requestedFieldsDontExistMode</span><span class="pi">:</span> <span class="s">SPARSE_ALL_FIELDS</span>

<span class="na">cd</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
  <span class="na">maxHops</span><span class="pi">:</span> <span class="m">3</span>
  <span class="na">maxIncludedResources</span><span class="pi">:</span> <span class="m">100</span>
  <span class="na">mapping</span><span class="pi">:</span>
    <span class="na">users</span><span class="pi">:</span> <span class="s">http://localhost:8080/jsonapi</span>
    <span class="na">countries</span><span class="pi">:</span> <span class="s">http://localhost:8080/jsonapi</span>

<span class="na">oas</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>
  <span class="na">info</span><span class="pi">:</span>
    <span class="na">title</span><span class="pi">:</span> <span class="s">My API</span>
    <span class="na">version</span><span class="pi">:</span> <span class="s">1.0.0</span></code></pre></div></div>
    <p>Note: the Servlet config file uses the property structure directly (no <code>jsonapi4j</code> prefix). Both <code>jsonapi4j.yaml</code> and <code>jsonapi4j.json</code> formats are supported.</p>
  </div>
</div>

## Config Loading Order (Servlet)

For Spring Boot and Quarkus, configuration loading follows each framework's standard rules (profiles, environment variables, system properties, etc.).

For the Servlet integration, JsonApi4j resolves configuration in the following priority order:

| Priority | Source | Example |
|----------|--------|---------|
| 1 | Servlet context attribute | Set programmatically or by Spring Boot / Quarkus auto-configuration |
| 2 | System property `jsonapi4j.config` | `-Djsonapi4j.config=/path/to/config.yaml` |
| 3 | Environment variable `JSONAPI4J_CONFIG` | `export JSONAPI4J_CONFIG=/path/to/config.yaml` |
| 4 | Servlet init parameter `jsonapi4j.config` | Set in `web.xml` or programmatically via `context.setInitParameter(...)` |
| 5 | Classpath defaults | `jsonapi4j.yaml` or `jsonapi4j.json` on the classpath |

## Core Properties

| Property | Default | Description |
|----------|---------|-------------|
| `jsonapi4j.rootPath` | `/jsonapi` | Root path for all JsonApi4j endpoints. All resource and relationship URLs are served under this path. Must be an absolute path with no trailing slash — it is mounted as a servlet mapping, and is [validated at startup](#startup-validation). |
| `jsonapi4j.meta.enabled` | `false` | Enables the built-in [Meta API](/meta-api/) — a runtime introspection endpoint exposing the app's resources, relationships, operations, plugins, and effective config. Opt-in. |

## Validation Properties

JsonApi4j includes a built-in structural validator that enforces limits on common request parameters. These properties configure the thresholds used by the built-in validator. Every limit must be greater than zero — a zero rejects every request carrying the parameter it caps rather than lifting the cap — and this is [checked at startup](#startup-validation).

| Property | Default | Description |
|----------|---------|-------------|
| `jsonapi4j.validation.maxNumberFilterParams` | `5` | Maximum number of distinct `filter[*]` query parameters allowed in a single request. |
| `jsonapi4j.validation.maxElementsInFilterParam` | `20` | Maximum number of comma-separated values within a single `filter[name]` parameter. |
| `jsonapi4j.validation.resourceIdMaxLength` | `64` | Maximum allowed length of a resource ID string. |
| `jsonapi4j.validation.limitMaxValue` | `100` | Maximum allowed value for the `page[limit]` pagination parameter. |
| `jsonapi4j.validation.maxElementsInIncludeParam` | `10` | Maximum number of relationship paths in the `include` query parameter. |
| `jsonapi4j.validation.maxElementsInSortByParam` | `5` | Maximum number of fields in the `sort` query parameter. |

## Plugin Properties

Each plugin adds its own properties under the `jsonapi4j` namespace. Refer to the corresponding plugin page for the full list:

| Plugin | Prefix | Documentation |
|--------|--------|---------------|
| Access Control | `jsonapi4j.ac.*` | [Access Control Plugin](/access-control-plugin/#available-properties) |
| Sparse Fieldsets | `jsonapi4j.sf.*` | [Sparse Fieldsets Plugin](/sparse-fieldsets-plugin/#available-properties) |
| OpenAPI | `jsonapi4j.oas.*` | [OpenAPI Plugin](/openapi-plugin/#available-properties) |
| Compound Documents | `jsonapi4j.cd.*` | [Compound Documents Plugin](/compound-docs-plugin/#available-properties) |

### Startup Validation

Every configuration section validates itself when the application starts — the root `jsonapi4j` section
first, then each enabled plugin. Errors are collected per section and reported all at once, each pointing
at the exact property key, so one restart is enough to see everything wrong with a section.

Root configuration problems fail the boot with a `RootConfigMisconfigurationException`:

```
JsonApi4j root configuration ('jsonapi4j') is invalid. Fix the configuration and restart:
Property errors:
  - 'jsonapi4j.rootPath': must start with '/', but was 'jsonapi'
```

Plugin problems fail it with a `PluginMisconfigurationException`, listing every misconfigured plugin:

```
Registered plugins are misconfigured. Fix the configuration and restart:
JsonApiCompoundDocsPlugin ('jsonapi4j.cd'):
Property errors:
  - 'jsonapi4j.cd.maxHops': must be greater than 0, but was 0
  - 'jsonapi4j.cd.mapping.users': must be an absolute http(s) URL with a host, but was '/jsonapi'
Cross-properties errors:
  - 'jsonapi4j.cd.httpTotalTimeoutMs' (1000) must not be less than 'jsonapi4j.cd.httpConnectTimeoutMs' (5000): the total budget of an include call has to cover connecting to the remote service
```

The checks cover mandatory values, ranges, URL and path formats, and combinations that are individually
valid but contradict each other — mutually exclusive license fields, two OAuth2 flows sharing one name,
duplicate response-header status codes. A plugin is also checked **against the root configuration**, which
is how a plugin endpoint claiming the same path as `jsonapi4j.rootPath` is caught. A **disabled** plugin is
never validated — parked configuration does not break a boot.

The root section is validated before the plugins, because plugins are checked against it: a broken
`rootPath` would otherwise produce a page of misleading follow-up errors.

Custom configuration gets the same treatment. Both `JsonApi4jProperties` and every plugin's
`PluginProperties` extend `ValidatableProperties`: override `validate()` and return the collected
`PropertiesValidationResult`, built with the shared `requireNotBlank` / `requirePositive` /
`requireHttpUrl` / `requireServletPath` vocabulary. A plugin that also has to agree with the root
configuration overrides `validateAgainst(JsonApi4jProperties)` instead.

### A config file with everything in it

Each sample application ships a configuration file listing every property with its default, so the whole
surface is visible in one place rather than assembled from tables:

| Framework | File |
|-----------|------|
| Spring Boot | `examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml` |
| Quarkus | `examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties` |
| Servlet | `examples/jsonapi4j-servlet-sampleapp/src/main/resources/jsonapi4j.yaml` |

Copy one and delete what you do not need — every value shown is the default, so removing a line changes
nothing.

## Overriding Beans

Both Spring Boot and Quarkus allow you to override any default bean provided by JsonApi4j.

**Spring Boot** — define a bean of the same type in a `@Configuration` class. JsonApi4j uses `@ConditionalOnMissingBean` on all defaults.

```java
@Configuration
public class CustomConfig {

    @Bean
    public ExecutorService jsonApi4jExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

**Quarkus** — provide a `@Produces` method. JsonApi4j beans are annotated with `@DefaultBean`, so your producer takes precedence.

```java
public class CustomConfig {

    @Produces
    @Singleton
    public ExecutorService jsonApi4jExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

**Servlet** — set custom instances as `ServletContext` attributes before the framework initializes. JsonApi4j checks the servlet context for known attributes (e.g. `JsonApi4j`, `PrincipalResolver`, `ErrorHandlerFactoriesRegistry`, `ExecutorService`, `ObjectMapper`) and uses them if present; otherwise it falls back to defaults.

```java
// Build and register a custom JsonApi4j instance
JsonApi4j jsonApi4j = JsonApi4j.builder()
        .domainRegistry(domainRegistry)
        .operationsRegistry(operationsRegistry)
        .plugins(PluginRegistry.builder().registerAll(plugins).build())
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();
servletContext.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

// Override PrincipalResolver
servletContext.setAttribute(JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME, myCustomPrincipalResolver);
```

See the [servlet sample app](https://github.com/MoonWorm/jsonapi4j/tree/main/examples/jsonapi4j-servlet-sampleapp) for a complete example.

### Overriding the PrincipalResolver

`PrincipalResolver` is the bean most applications end up overriding, because it decides who the caller is.
The default reads `X-Authenticated-*` headers; declaring your own bean replaces it through the same
mechanisms shown above.

**Spring Boot** — read claims Spring Security has already verified:

```java
@Bean
public PrincipalResolver jsonapi4jPrincipalResolver() {
    return SpringSecurityPrincipalResolver.withEntitlementsClaim("entitlements");
}
```

**Quarkus** — read claims Quarkus OIDC has already verified:

```java
@Produces
@Singleton
public PrincipalResolver principalResolver(JsonWebToken jwt) {
    return QuarkusJwtPrincipalResolver.withEntitlementsClaim(jwt, "entitlements");
}
```

Both are opt-in, so an existing header-based setup keeps working untouched after an upgrade. See
[Principal Resolution](/principal-resolution/) for the full set of resolvers, claim mapping, and the
security trade-offs between them.

### Overriding the AccessControlEvaluator

`AccessControlEvaluator` decides every access control requirement for the whole application. The default,
`DefaultAccessControlEvaluator`, evaluates `authenticated`, `entitlements`, `scopes`, `ownership` and
`policy` — each must pass.

**Reach for a policy first.** A single rule that the declarative requirements cannot express belongs in an
[`AccessPolicy`](/access-control-plugin/#policies-deciding-access-in-code) on the one annotation that needs
it. Replacing the evaluator makes you responsible for *all five* requirement types on *every* annotated
element in the application, which is rarely what you want:

```java
@AccessControl(policy = @AccessControlPolicy(MyRule.class))   // one rule, one place
```

Replace the evaluator only when you are changing how evaluation *itself* works — decisions delegated to an
external authorization service, an audit record for every decision, or caching.

**Extend the default rather than starting from scratch**, so the requirements you are not changing keep
working:

```java
public class AuditingAccessControlEvaluator extends DefaultAccessControlEvaluator {

    @Override
    public EvaluationResult evaluateInboundRequirements(AccessControlContext context,
                                                       AccessControlModel accessControlModel) {
        EvaluationResult result = super.evaluateInboundRequirements(context, accessControlModel);
        auditLog.record(context.principal().authenticatedUserId(), context.operation(), result.granted());
        return result;
    }
}
```

`EvaluationResult` carries the decision together with the `ErrorCode` to report when access is refused, so
a denial can say more than "forbidden" — the default evaluator reports `INSUFFICIENT_ENTITLEMENTS` or
`INSUFFICIENT_SCOPES` where those are what failed. Both come from the same evaluation, so the reason can
never disagree with the decision.

The code is an [`ErrorCode`](/error-handling/), not a fixed set of framework-defined reasons, so an
evaluator can report something meaningful in its own domain:

```java
if (subscriptionExpired(context)) {
    return EvaluationResult.denied(MyErrorCodes.SUBSCRIPTION_EXPIRED);
}
return EvaluationResult.allowed();
```

**Spring Boot** — the default is `@ConditionalOnMissingBean`, so your bean wins:

```java
@Bean
public AccessControlEvaluator jsonapi4jAccessControlEvaluator() {
    return new AuditingAccessControlEvaluator();
}
```

**Quarkus** — the default is a `@DefaultBean`, so your producer takes precedence:

```java
@Produces
@Singleton
public AccessControlEvaluator accessControlEvaluator() {
    return new AuditingAccessControlEvaluator();
}
```

**Servlet** — unlike `PrincipalResolver`, the evaluator is not read from a `ServletContext` attribute. Pass it
to the plugin when you build the plugin list:

```java
new JsonApiAccessControlPlugin(
        new AuditingAccessControlEvaluator(),
        DefaultAcProperties.toAcProperties(jsonApi4jPropertiesRaw)
)
```

Both evaluation methods receive an
[`AccessControlContext`](/access-control-plugin/#what-a-policy-can-see) carrying the principal, the operation,
the request and — outbound — the resource being emitted.

For a complete configuration example with all plugins enabled, see the sample application configs:
- [Spring Boot application.yaml](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml)
- [Quarkus application.properties](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties)
- [Servlet jsonapi4j.yaml](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-servlet-sampleapp/src/main/resources/jsonapi4j.yaml)
