package pro.api4.jsonapi4j.sampleapp.servlet;

import jakarta.validation.constraints.NotBlank;
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
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
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
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserCitizenshipsOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserPlaceOfBirthOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserRelativesOperations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ServletJsonapi4jSampleApp {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(context.getServletContext());
        OasProperties oasProperties = DefaultOasProperties.toOasProperties(jsonApi4jPropertiesRaw);
        List<JsonApi4jPlugin> plugins = List.of(
                new JsonApiAccessControlPlugin(
                        new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry()),
                        DefaultAcProperties.toAcProperties(jsonApi4jPropertiesRaw)
                ),
                new JsonApiSparseFieldsetsPlugin(DefaultSfProperties.toSfProperties(jsonApi4jPropertiesRaw)),
                new JsonApiOasPlugin(oasProperties),
                new JsonApiCompoundDocsPlugin()
        );

        UserDb userDb = new UserInMemoryDb();
        var domainRegistry = DomainRegistry.builder(plugins)
                .resource(new UserResource())
                .resource(new CountryResource())
                .resource(new CurrencyResource())
                .relationship(new UserCitizenshipsRelationship())
                .relationship(new UserPlaceOfBirthRelationship())
                .relationship(new UserRelativesRelationship())
                .relationship(new CountryCurrenciesRelationship())
                .build();

        UserInputParamsValidator userInputParamsValidator = new UserInputParamsValidator() {
            @Override
            public void validateFirstName(String firstName) {

            }

            @Override
            public void validateLastName(String lastName) {

            }

            @Override
            public void validateEmail(String email) {

            }
        };
        CountryInputParamsValidator countryInputParamsValidator = new CountryInputParamsValidator() {

            @Override
            public void validateCountryId(String countryId) {

            }

            @Override
            public void validateCountryIds(List<@NotBlank String> countryIds) {

            }

            @Override
            public void validateRegion(String region) {

            }
        };

        CountriesClient countriesClient = new CountriesInMemoryClient();
        var operationsRegistry = OperationsRegistry.builder(plugins)
                .operation(new UserOperations(userDb, userInputParamsValidator, countryInputParamsValidator))
                .operation(new UserCitizenshipsOperations(countriesClient, userDb, countryInputParamsValidator))
                .operation(new UserPlaceOfBirthOperations(countriesClient, userDb, countryInputParamsValidator))
                .operation(new UserRelativesOperations(userDb))
                .operation(new ReadCountryByIdOperation(countriesClient, countryInputParamsValidator))
                .operation(new ReadMultipleCountriesOperation(countriesClient, countryInputParamsValidator))
                .operation(new ReadCountryCurrenciesRelationshipOperation(countriesClient, countryInputParamsValidator))
                .operation(new CurrencyOperations(countriesClient))
                .build();

        var jsonApi4j = JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(plugins)
                .executor(Executors.newCachedThreadPool())
                .build();

        context.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

        var jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();
        jsonApi4jInitializer.onStartup(null, context.getServletContext());

        var oasPluginInitializer = new JsonApiOasServletContainerInitializer();
        oasPluginInitializer.onStartup(null, context.getServletContext());

        server.setHandler(context);
        server.start();
        server.join();
    }
}
