package pro.api4.jsonapi4j.sampleapp.servlet;

import jakarta.servlet.ServletContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.MetaConfigComposer;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.ac.config.DefaultAcProperties;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin;
import pro.api4.jsonapi4j.plugin.sf.config.DefaultSfProperties;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesInMemoryClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.UserInMemoryDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryCurrenciesRelationshipOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserCitizenshipsOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserPlaceOfBirthOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserRelativesOperations;

import java.util.List;
import java.util.Map;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;
import static pro.api4.jsonapi4j.config.Integration.SERVLET;

public class ServletJsonapi4jSampleApp {

    static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");

        JsonApi4jServletContainerInitializer jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();

        List<JsonApi4jPlugin> plugins = initPlugins(context.getServletContext());

        initMetaContext(plugins, context.getServletContext());

        initDomainRegistry(plugins, context.getServletContext());
        initOperationRegistry(plugins, context.getServletContext());

        jsonApi4jInitializer.onStartup(null, context.getServletContext());

        // start server
        server.setHandler(context);
        server.start();
        server.join();
    }

    static List<JsonApi4jPlugin> initPlugins(ServletContext servletContext) {
        // get full config as raw map
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadRawConfig(servletContext).getProperties();

        // init Compound Docs plugin
        JsonApi4jCompoundDocsServletContainerInitializer cdPluginInitializer = new JsonApi4jCompoundDocsServletContainerInitializer();
        cdPluginInitializer.onStartup(null, servletContext);

        // init OAS plugin
        JsonApiOasServletContainerInitializer oasPluginInitializer = new JsonApiOasServletContainerInitializer();
        oasPluginInitializer.onStartup(null, servletContext);

        // build plugins
        List<JsonApi4jPlugin> plugins = List.of(
                new JsonApiAccessControlPlugin(
                        new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry()),
                        DefaultAcProperties.toAcProperties(jsonApi4jPropertiesRaw)
                ),
                new JsonApiSparseFieldsetsPlugin(DefaultSfProperties.toSfProperties(jsonApi4jPropertiesRaw)),
                new JsonApiOasPlugin(DefaultOasProperties.toOasProperties(jsonApi4jPropertiesRaw)),
                new JsonApiCompoundDocsPlugin(DefaultCompoundDocsProperties.toCdProperties(jsonApi4jPropertiesRaw))
        );

        // set to the context
        servletContext.setAttribute(PLUGINS_ATT_NAME, plugins);

        return plugins;
    }

    static void initDomainRegistry(List<JsonApi4jPlugin> plugins,
                                   ServletContext servletContext) {
        DomainRegistry domainRegistry = DomainRegistry.builder(plugins)
                .resource(new UserResource())
                .resource(new CountryResource())
                .resource(new CurrencyResource())
                .relationship(new UserCitizenshipsRelationship())
                .relationship(new UserPlaceOfBirthRelationship())
                .relationship(new UserRelativesRelationship())
                .relationship(new CountryCurrenciesRelationship())
                .build();
        servletContext.setAttribute(DOMAIN_REGISTRY_ATT_NAME, domainRegistry);
    }

    static void initOperationRegistry(List<JsonApi4jPlugin> plugins,
                                      ServletContext servletContext) {
        UserDb userDb = new UserInMemoryDb();
        CountriesClient countriesClient = new CountriesInMemoryClient();
        OperationsRegistry operationsRegistry = OperationsRegistry.builder(plugins)
                .operation(new UserOperations(userDb))
                .operation(new UserCitizenshipsOperations(userDb))
                .operation(new UserPlaceOfBirthOperations(userDb))
                .operation(new UserRelativesOperations(userDb))
                .operation(new ReadCountryByIdOperation(countriesClient))
                .operation(new ReadMultipleCountriesOperation(countriesClient))
                .operation(new ReadCountryCurrenciesRelationshipOperation(countriesClient))
                .operation(new CurrencyOperations(countriesClient))
                .build();

        servletContext.setAttribute(OPERATION_REGISTRY_ATT_NAME, operationsRegistry);
    }

    static void initMetaContext(List<JsonApi4jPlugin> plugins, ServletContext servletContext) {
        JsonApi4jProperties properties = JsonApi4jPropertiesLoader.loadConfig(servletContext);
        if (!properties.meta().enabled()) {
            return;
        }
        MetaContext metaContext = MetaContext.of(MetaConfigComposer.compose(properties, plugins), SERVLET);
        servletContext.setAttribute(META_CONTEXT_ATT_NAME, metaContext);
    }

}
