package pro.api4.jsonapi4j.sampleapp.quarkus.operations.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryCurrenciesRelationshipOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.*;

@ApplicationScoped
public class OperationsQuarkusConfig {

    @Singleton
    @Produces
    public CurrencyOperations currencyOperations(CountriesClient countriesClient) {
        return new CurrencyOperations(countriesClient);
    }

    @Singleton
    @Produces
    public UserOperations userOperations(
            UserDb userDb,
            UserInputParamsValidator userValidator,
            CountryInputParamsValidator countryValidator
    ) {
        return new UserOperations(userDb, userValidator, countryValidator);
    }

    @Singleton
    @Produces
    public UserCitizenshipsOperations userCitizenshipsOperations(
            CountriesClient countriesClient,
            UserDb userDb,
            CountryInputParamsValidator validator
    ) {
        return new UserCitizenshipsOperations(countriesClient, userDb, validator);
    }

    @Singleton
    @Produces
    public UserPlaceOfBirthOperations userPlaceOfBirthOperations(
            CountriesClient client,
            UserDb userDb
    ) {
        return new UserPlaceOfBirthOperations(client, userDb);
    }

    @Singleton
    @Produces
    public UserRelativesOperations userRelativesOperations(UserDb userDb) {
        return new UserRelativesOperations(userDb);
    }

    @Singleton
    @Produces
    public ReadCountryByIdOperation readCountryByIdOperation(CountriesClient countriesClient,
                                                             CountryInputParamsValidator validator) {
        return new ReadCountryByIdOperation(countriesClient, validator);
    }

    @Singleton
    @Produces
    public ReadMultipleCountriesOperation readMultipleCountriesOperation(CountriesClient countriesClient,
                                                                         CountryInputParamsValidator validator) {
        return new ReadMultipleCountriesOperation(countriesClient, validator);
    }

    @Singleton
    @Produces
    public ReadCountryCurrenciesRelationshipOperation readCountryCurrenciesRelationshipOperation(
            CountriesClient countriesClient,
            CountryInputParamsValidator validator) {
        return new ReadCountryCurrenciesRelationshipOperation(countriesClient, validator);
    }

}
