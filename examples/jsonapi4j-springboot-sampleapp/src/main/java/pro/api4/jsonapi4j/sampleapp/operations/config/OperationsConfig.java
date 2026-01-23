package pro.api4.jsonapi4j.sampleapp.operations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.sampleapp.config.datasource.IntegrationsConfig;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryCurrenciesRelationshipOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.*;

@Configuration
@Import(IntegrationsConfig.class)
public class OperationsConfig {

    @Bean
    public CurrencyOperations currencyOperations(CountriesClient countriesClient) {
        return new CurrencyOperations(countriesClient);
    }

    @Bean
    public UserOperations userOperations(
            UserDb userDb,
            UserInputParamsValidator userValidator,
            CountryInputParamsValidator countryValidator
    ) {
        return new UserOperations(userDb, userValidator, countryValidator);
    }

    @Bean
    public UserCitizenshipsOperations userCitizenshipsOperations(
            CountriesClient countriesClient,
            UserDb userDb,
            CountryInputParamsValidator validator
    ) {
        return new UserCitizenshipsOperations(countriesClient, userDb, validator);
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
    public ReadCountryByIdOperation readCountryByIdOperation(CountriesClient countriesClient,
                                                             CountryInputParamsValidator validator) {
        return new ReadCountryByIdOperation(countriesClient, validator);
    }

    @Bean
    public ReadMultipleCountriesOperation readMultipleCountriesOperation(CountriesClient countriesClient,
                                                                         CountryInputParamsValidator validator) {
        return new ReadMultipleCountriesOperation(countriesClient, validator);
    }

    @Bean
    public ReadCountryCurrenciesRelationshipOperation readCountryCurrenciesRelationshipOperation(
            CountriesClient countriesClient,
            CountryInputParamsValidator validator) {
        return new ReadCountryCurrenciesRelationshipOperation(countriesClient, validator);
    }

}
