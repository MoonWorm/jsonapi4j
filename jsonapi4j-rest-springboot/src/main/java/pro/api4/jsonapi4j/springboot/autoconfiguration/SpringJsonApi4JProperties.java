package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;

import static pro.api4.jsonapi4j.config.JsonApi4jProperties.CONFIG_PREFIX;

@Component
@ConfigurationProperties(prefix = CONFIG_PREFIX)
public class SpringJsonApi4JProperties extends DefaultJsonApi4jProperties {

}
