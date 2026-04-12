package pro.api4.jsonapi4j.sampleapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.sampleapp.config.domain.SpringJsonApi4jDomainConfig;
import pro.api4.jsonapi4j.sampleapp.config.operations.SpringJsonApi4jOperationsConfig;

@Configuration
@Import(value = {
        SpringJsonApi4jDomainConfig.class,
        SpringJsonApi4jOperationsConfig.class
})
public class SpringJsonApi4jConfig {
}
