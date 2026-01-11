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
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyRepository;
import pro.api4.jsonapi4j.sampleapp.operations.user.*;

@Configuration
@Import(IntegrationsConfig.class)
public class OperationsConfig {

    @Bean
    public CurrencyRepository currencyRepository(CountriesClient countriesClient) {
        return new CurrencyRepository(countriesClient);
    }

    @Bean
    public UserRepository userRepository(
            UserDb userDb,
            UserInputParamsValidator userValidator,
            CountryInputParamsValidator countryValidator
    ) {
        return new UserRepository(userDb, userValidator, countryValidator);
    }

    @Bean
    public UserCitizenshipsRepository userCitizenshipsRepository(
            CountriesClient countriesClient,
            UserDb userDb,
            CountryInputParamsValidator validator
    ) {
        return new UserCitizenshipsRepository(countriesClient, userDb, validator);
    }

    @Bean
    public UserPlaceOfBirthRepository userPlaceOfBirthRepository(
            CountriesClient client,
            UserDb userDb
    ) {
        return new UserPlaceOfBirthRepository(client, userDb);
    }

    @Bean
    public UserRelativesRepository userRelativesRepository(UserDb userDb) {
        return new UserRelativesRepository(userDb);
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
