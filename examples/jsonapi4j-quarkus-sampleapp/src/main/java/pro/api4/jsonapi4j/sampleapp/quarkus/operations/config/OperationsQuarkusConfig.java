package pro.api4.jsonapi4j.sampleapp.quarkus.operations.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryCurrenciesRelationshipOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.currency.CurrencyOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserCitizenshipsOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserPlaceOfBirthOperations;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserRelativesOperations;

@ApplicationScoped
public class OperationsQuarkusConfig {

    @ApplicationScoped
    @Produces
    public CurrencyOperations currencyOperations(CountriesClient countriesClient) {
        return new CurrencyOperations(countriesClient);
    }

    @ApplicationScoped
    @Produces
    public UserOperations userOperations(UserDb userDb) {
        return new UserOperations(userDb);
    }

    @ApplicationScoped
    @Produces
    public UserCitizenshipsOperations userCitizenshipsOperations(UserDb userDb) {
        return new UserCitizenshipsOperations(userDb);
    }

    @ApplicationScoped
    @Produces
    public UserPlaceOfBirthOperations userPlaceOfBirthOperations(UserDb userDb) {
        return new UserPlaceOfBirthOperations(userDb);
    }

    @ApplicationScoped
    @Produces
    public UserRelativesOperations userRelativesOperations(UserDb userDb) {
        return new UserRelativesOperations(userDb);
    }

    @ApplicationScoped
    @Produces
    public ReadCountryByIdOperation readCountryByIdOperation(CountriesClient countriesClient) {
        return new ReadCountryByIdOperation(countriesClient);
    }

    @ApplicationScoped
    @Produces
    public ReadMultipleCountriesOperation readMultipleCountriesOperation(CountriesClient countriesClient) {
        return new ReadMultipleCountriesOperation(countriesClient);
    }

    @ApplicationScoped
    @Produces
    public ReadCountryCurrenciesRelationshipOperation readCountryCurrenciesRelationshipOperation(
            CountriesClient countriesClient) {
        return new ReadCountryCurrenciesRelationshipOperation(countriesClient);
    }

}
