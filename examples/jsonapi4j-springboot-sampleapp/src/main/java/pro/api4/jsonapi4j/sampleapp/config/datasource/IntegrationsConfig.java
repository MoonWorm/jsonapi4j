package pro.api4.jsonapi4j.sampleapp.config.datasource;

import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients(clients = RestCountriesFeignClient.class)
@Configuration
public class IntegrationsConfig {

}
