package pro.api4.jsonapi4j.sampleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.sampleapp.config.datasource.IntegrationsConfig;
import pro.api4.jsonapi4j.sampleapp.domain.config.DomainSpringConfig;
import pro.api4.jsonapi4j.sampleapp.operations.config.OperationsConfig;

@Import({DomainSpringConfig.class, OperationsConfig.class})
@SpringBootApplication
public class Jsonapi4jSampleApp {

    public static void main(String[] args) {
        SpringApplication.run(Jsonapi4jSampleApp.class, args);
    }

}
