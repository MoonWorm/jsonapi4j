package pro.api4.jsonapi4j.sampleapp.servlet;

import jakarta.servlet.ServletContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.ac.config.DefaultAcProperties;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
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
import pro.api4.jsonapi4j.sampleapp.operations.user.*;
import pro.api4.jsonapi4j.sampleapp.servlet.validation.SimpleCountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.servlet.validation.SimpleUserInputParamsValidator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ServletJsonapi4jSampleApp {

    static List<JsonApi4jPlugin> initPlugins(ServletContext servletContext) {
        // load props
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);

        // init Compound Docs plugin
        JsonApi4jCompoundDocsServletContainerInitializer cdPluginInitializer = new JsonApi4jCompoundDocsServletContainerInitializer();
        cdPluginInitializer.onStartup(null, servletContext);

        // init OAS plugin
        JsonApiOasServletContainerInitializer oasPluginInitializer = new JsonApiOasServletContainerInitializer();
        oasPluginInitializer.onStartup(null, servletContext);

        // build plugins
        return List.of(
                new JsonApiAccessControlPlugin(
                        new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry()),
                        DefaultAcProperties.toAcProperties(jsonApi4jPropertiesRaw)
                ),
                new JsonApiSparseFieldsetsPlugin(DefaultSfProperties.toSfProperties(jsonApi4jPropertiesRaw)),
                new JsonApiOasPlugin(DefaultOasProperties.toOasProperties(jsonApi4jPropertiesRaw)),
                new JsonApiCompoundDocsPlugin()
        );
    }

    private static DomainRegistry createDomainRegistry(List<JsonApi4jPlugin> plugins) {
        return DomainRegistry.builder(plugins)
                .resource(new UserResource())
                .resource(new CountryResource())
                .resource(new CurrencyResource())
                .relationship(new UserCitizenshipsRelationship())
                .relationship(new UserPlaceOfBirthRelationship())
                .relationship(new UserRelativesRelationship())
                .relationship(new CountryCurrenciesRelationship())
                .build();
    }

    static JsonApi4j createJsonApi4j(List<JsonApi4jPlugin> plugins) {
        DomainRegistry domainRegistry = createDomainRegistry(plugins);
        OperationsRegistry operationsRegistry = createOperationRegistry(plugins);
        return JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(plugins)
                .executor(Executors.newCachedThreadPool())
                .build();
    }

    private static OperationsRegistry createOperationRegistry(List<JsonApi4jPlugin> plugins) {
        UserDb userDb = new UserInMemoryDb();
        CountriesClient countriesClient = new CountriesInMemoryClient();
        UserInputParamsValidator userInputParamsValidator = new SimpleUserInputParamsValidator();
        SimpleCountryInputParamsValidator countryInputParamsValidator = new SimpleCountryInputParamsValidator();

        return OperationsRegistry.builder(plugins)
                .operation(new UserOperations(userDb, userInputParamsValidator, countryInputParamsValidator))
                .operation(new UserCitizenshipsOperations(countriesClient, userDb, countryInputParamsValidator))
                .operation(new UserPlaceOfBirthOperations(countriesClient, userDb, countryInputParamsValidator))
                .operation(new UserRelativesOperations(userDb))
                .operation(new ReadCountryByIdOperation(countriesClient, countryInputParamsValidator))
                .operation(new ReadMultipleCountriesOperation(countriesClient, countryInputParamsValidator))
                .operation(new ReadCountryCurrenciesRelationshipOperation(countriesClient, countryInputParamsValidator))
                .operation(new CurrencyOperations(countriesClient))
                .build();
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");

        // init dispatcher servlet
        JsonApi4jServletContainerInitializer jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();
        jsonApi4jInitializer.onStartup(null, context.getServletContext());

        // init plugins
        List<JsonApi4jPlugin> plugins = initPlugins(context.getServletContext());

        // compose and register JsonApi4j
        JsonApi4j jsonApi4j = createJsonApi4j(plugins);
        context.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

        // start server
        server.setHandler(context);
        server.start();
        server.join();
    }
}
