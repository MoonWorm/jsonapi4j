package pro.api4.jsonapi4j.sampleapp.quarkus.config.datasource;

import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesInMemoryClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.UserInMemoryDb;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class IntegrationsConfig {

    @Produces
    @ApplicationScoped
    public UserDb userDb() {
        return new UserInMemoryDb();
    }

    @Produces
    @ApplicationScoped
    public CountriesClient countriesClient() {
        return new CountriesInMemoryClient();
    }

}
