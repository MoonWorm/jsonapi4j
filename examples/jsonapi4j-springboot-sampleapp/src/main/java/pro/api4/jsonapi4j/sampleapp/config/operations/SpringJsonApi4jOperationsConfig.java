package pro.api4.jsonapi4j.sampleapp.config.operations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.sampleapp.config.datasource.IntegrationsConfig;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryCurrenciesRelationshipOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.*;

@Configuration
@Import(IntegrationsConfig.class)
public class SpringJsonApi4jOperationsConfig {

    @Bean
    public CurrencyOperations currencyOperations(CountriesClient countriesClient) {
        return new CurrencyOperations(countriesClient);
    }

    @Bean
    public UserOperations userOperations(
            UserDb userDb,
            UserInputParamsValidator userValidator
    ) {
        return new UserOperations(userDb, userValidator);
    }

    @Bean
    public UserCitizenshipsOperations userCitizenshipsOperations(
            CountriesClient countriesClient,
            UserDb userDb
    ) {
        return new UserCitizenshipsOperations(countriesClient, userDb);
    }

    @Bean
    public UserPlaceOfBirthOperations userPlaceOfBirthOperations(
            CountriesClient client,
            UserDb userDb
    ) {
        return new UserPlaceOfBirthOperations(client, userDb);
    }

    @Bean
    public UserRelativesOperations userRelativesOperations(UserDb userDb) {
        return new UserRelativesOperations(userDb);
    }

    @Bean
    public ReadCountryByIdOperation readCountryByIdOperation(CountriesClient countriesClient) {
        return new ReadCountryByIdOperation(countriesClient);
    }

    @Bean
    public ReadMultipleCountriesOperation readMultipleCountriesOperation(CountriesClient countriesClient) {
        return new ReadMultipleCountriesOperation(countriesClient);
    }

    @Bean
    public ReadCountryCurrenciesRelationshipOperation readCountryCurrenciesRelationshipOperation(
            CountriesClient countriesClient) {
        return new ReadCountryCurrenciesRelationshipOperation(countriesClient);
    }

}
