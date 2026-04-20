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

  <span class="na">ac</span><span class="pi">:</span>
    <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>

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

<span class="py">jsonapi4j.ac.enabled</span>=<span class="s">true</span>

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
    <p>Quarkus uses Smallrye Config for property resolution. Lists use indexed syntax: <code>jsonapi4j.cd.propagation[0]=FIELDS</code>. All JsonApi4j CDI beans use <code>@DefaultBean</code>, so you can override them with your own <code>@Produces</code> methods.</p>
  </div>

  <div id="cfg-servlet" class="tab-panel">
    <p>For plain Servlet applications, JsonApi4j loads configuration from a YAML or JSON file. No framework-specific property binding is used.</p>
    <p><strong>jsonapi4j.yaml</strong> (on the classpath):</p>
    <div class="language-yaml highlighter-rouge"><div class="highlight"><pre class="highlight"><code><span class="na">rootPath</span><span class="pi">:</span> <span class="s">/jsonapi</span>

<span class="na">ac</span><span class="pi">:</span>
  <span class="na">enabled</span><span class="pi">:</span> <span class="no">true</span>

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
| `jsonapi4j.rootPath` | `/jsonapi` | Root path for all JsonApi4j endpoints. All resource and relationship URLs are served under this path. |

## Plugin Properties

Each plugin adds its own properties under the `jsonapi4j` namespace. Refer to the corresponding plugin page for the full list:

| Plugin | Prefix | Documentation |
|--------|--------|---------------|
| Access Control | `jsonapi4j.ac.*` | [Access Control Plugin](/access-control-plugin/#available-properties) |
| Sparse Fieldsets | `jsonapi4j.sf.*` | [Sparse Fieldsets Plugin](/sparse-fieldsets-plugin/#available-properties) |
| OpenAPI | `jsonapi4j.oas.*` | [OpenAPI Plugin](/openapi-plugin/#available-properties) |
| Compound Documents | `jsonapi4j.cd.*` | [Compound Documents Plugin](/compound-docs-plugin/#available-properties) |

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
        .plugins(plugins)
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();
servletContext.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

// Override PrincipalResolver
servletContext.setAttribute(JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME, myCustomPrincipalResolver);
```

See the [servlet sample app](https://github.com/MoonWorm/jsonapi4j/tree/main/examples/jsonapi4j-servlet-sampleapp) for a complete example.

For a complete configuration example with all plugins enabled, see the sample application configs:
- [Spring Boot application.yaml](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-springboot-sampleapp/src/main/resources/application.yaml)
- [Quarkus application.properties](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-quarkus-sampleapp/src/main/resources/application.properties)
- [Servlet jsonapi4j.yaml](https://github.com/MoonWorm/jsonapi4j/blob/main/examples/jsonapi4j-servlet-sampleapp/src/main/resources/jsonapi4j.yaml)
