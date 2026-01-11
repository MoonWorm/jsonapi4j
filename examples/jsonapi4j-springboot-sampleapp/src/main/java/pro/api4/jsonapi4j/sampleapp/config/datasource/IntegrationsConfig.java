package pro.api4.jsonapi4j.sampleapp.config.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesInMemoryClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.UserInMemoryDb;

//@EnableFeignClients(clients = RestCountriesFeignClient.class)
@Configuration
public class IntegrationsConfig {

    @Bean
    public UserDb userDb() {
        return new UserInMemoryDb();
    }

    @Bean
    public CountriesClient countriesClient() {
        return new CountriesInMemoryClient();
    }

}
