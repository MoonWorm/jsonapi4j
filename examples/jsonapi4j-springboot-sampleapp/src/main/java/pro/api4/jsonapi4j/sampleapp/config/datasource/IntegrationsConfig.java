package pro.api4.jsonapi4j.sampleapp.config.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

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
