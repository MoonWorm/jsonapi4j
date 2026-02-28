package pro.api4.jsonapi4j.sampleapp.quarkus.domain.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;

@ApplicationScoped
public class DomainQuarkusConfig {

    @Produces
    public CountryResource countryResource() {
        return new CountryResource();
    }

    @Produces
    public UserResource userResource() {
        return new UserResource();
    }

    @Produces
    public CurrencyResource currencyResource() {
        return new CurrencyResource();
    }

    @Produces
    public CountryCurrenciesRelationship countryCurrenciesRelationship() {
        return new CountryCurrenciesRelationship();
    }

    @Produces
    public UserCitizenshipsRelationship userCitizenshipsRelationship() {
        return new UserCitizenshipsRelationship();
    }

    @Produces
    public UserPlaceOfBirthRelationship userPlaceOfBirthRelationship() {
        return new UserPlaceOfBirthRelationship();
    }

    @Produces
    public UserRelativesRelationship userRelativesRelationship() {
        return new UserRelativesRelationship();
    }

}
