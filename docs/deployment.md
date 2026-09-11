---
title: "Deployment"
permalink: /deployment/
---

How to run a JsonApi4j application: which servlet containers it works with, the one container setting that is
required, and how to deploy as a WAR rather than an executable jar.

If you are following the [Quick Start Guide](/getting-started/) on Spring Boot or Quarkus with an embedded
container, you do not need anything on this page — the one exception is the Tomcat setting below.

## Servlet Containers

**JsonApi4j** does not tie you to a particular servlet container. The Spring Boot integration runs on
whichever container your application already uses — **Tomcat**, **Jetty** and **Undertow** are each verified
against the full test suite. Deploying as a WAR leaves the choice to whichever container you deploy into.

Quarkus is the exception, and in a good way: there is nothing to choose. The `jsonapi4j-rest-quarkus`
extension builds on Quarkus' own servlet support (`quarkus-undertow`), which it brings in itself, so the
container question never arises — and neither does the Tomcat note below.

**On Tomcat, one setting is required.** JSON:API sends `filter[...]` and `fields[...]` unencoded, and Tomcat
rejects raw `[` and `]` in a query string by default — requests come back as `400 Bad Request`. Tell Tomcat to
accept them:

```yaml
server:
  tomcat:
    relaxed-query-chars: "[,]"
```

Jetty, Undertow and Quarkus accept those characters as they are and need no equivalent. The framework does not
configure your container for you: it is your connector, and a library reaching into it is surprising when you
later set the same property yourself.

## Deploying as a WAR

The `jsonapi4j-rest` module ships a `ServletContainerInitializer`, so a servlet container discovers and runs it
on deployment. No `web.xml` entry is needed — the dispatcher servlet and the framework filters register
themselves under the configured `rootPath`.

Configuration is picked up from the classpath, so packaging `jsonapi4j.yaml` (or `jsonapi4j.json`) in
`WEB-INF/classes` is enough. A `jsonapi4j.config` system property, a `JSONAPI4J_CONFIG` environment variable,
or a `jsonapi4j.config` servlet context init parameter all take precedence if you would rather point elsewhere.

What the container cannot discover is your domain. Contribute it from a `ServletContextListener`:

```java
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.DOMAIN_REGISTRY_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.OPERATION_REGISTRY_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.PLUGIN_REGISTRY_ATT_NAME;

@WebListener
public class JsonApi4jBootstrapListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();

        PluginRegistry plugins = PluginRegistry.empty();

        DomainRegistry domainRegistry = DomainRegistry.builder(plugins)
                .resource(new UserResource())
                .relationship(new UserCitizenshipsRelationship())
                .build();

        OperationsRegistry operationsRegistry = OperationsRegistry.builder(plugins)
                .operation(new UserOperations(userDb))
                .build();

        servletContext.setAttribute(PLUGIN_REGISTRY_ATT_NAME, plugins);
        servletContext.setAttribute(DOMAIN_REGISTRY_ATT_NAME, domainRegistry);
        servletContext.setAttribute(OPERATION_REGISTRY_ATT_NAME, operationsRegistry);
    }
}
```

A listener is the right place because of ordering: the container runs every `ServletContainerInitializer`
first, then notifies listeners, and only then initializes servlets. Your listener is therefore the earliest
application code that runs, and it still lands before the dispatcher servlet reads the registries. Registering
plugins here works the same way — build them into the `PluginRegistry` above.

A listener can also replace the framework's defaults — register your own `PrincipalResolver`, `ObjectMapper`
or `ErrorHandlerFactoriesRegistry` under the matching attribute and it takes effect, because the dispatcher
servlet and the filters read those attributes when they initialize, which is after listeners have run.

Configuration is the exception. `rootPath` is read during deployment to map the dispatcher servlet and the
framework filters, so setting `JsonApi4jProperties` from a listener is too late to move the endpoints — and
leaves the generated links pointing somewhere the servlet is not mapped. Configure the framework through the
config file, system property, environment variable or init parameter described above; use the listener for
your domain, not for `rootPath`.

If the listener is missing, the endpoints still register but serve an empty API, and the startup log reports
which registries were not found.

On an external Tomcat there is no `application.yaml` to carry the query-character setting described above —
set it on the `<Connector>` in `server.xml` instead:

```xml
<Connector port="8080" protocol="HTTP/1.1" relaxedQueryChars="[]" />
```

## Native Image

Quarkus applications can be built as GraalVM native executables, and the framework is verified that way on a
schedule. One framework concern needs attention: the Access Control plugin instantiates your attributes class
reflectively, which a native image permits only for classes registered ahead of time. The Quarkus extension
registers them by scanning for `@AccessControl` at build time, but Quarkus indexes your application module and
**not its dependencies** — so attributes classes living in a separate jar need a Jandex index in that jar.

See [Native Image in the Access Control plugin docs](/access-control-plugin/#native-image-graalvm--quarkus)
for the Jandex configuration and the alternative for third-party jars you cannot modify.

## Where to go next

- [Configuration](/configuration/) — every property, and how to override the framework's default beans.
- [Compatibility](/compatibility/) — which container, Spring Boot, Quarkus and Java versions are supported.
- [Principal Resolution](/principal-resolution/) — wiring authentication into a WAR deployment.
