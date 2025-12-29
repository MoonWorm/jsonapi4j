package pro.api4.jsonapi4j.sampleapp.domain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;

@Configuration
public class DomainSpringConfig {

    @Bean
    public CountryResource countryResource() {
        return new CountryResource();
    }

    @Bean
    public UserResource userResource() {
        return new UserResource();
    }

    @Bean
    public CurrencyResource currencyResource() {
        return new CurrencyResource();
    }

    @Bean
    public CountryCurrenciesRelationship countryCurrenciesRelationship() {
        return new CountryCurrenciesRelationship();
    }

    @Bean
    public UserCitizenshipsRelationship userCitizenshipsRelationship() {
        return new UserCitizenshipsRelationship();
    }

    @Bean
    public UserPlaceOfBirthRelationship userPlaceOfBirthRelationship() {
        return new UserPlaceOfBirthRelationship();
    }

    @Bean
    public UserRelativesRelationship userRelativesRelationship() {
        return new UserRelativesRelationship();
    }

}
