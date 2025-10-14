package pro.api4.jsonapi4j.springboot.autoconfiguration;

import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jsonapi4j")
public class SpringJsonApi4JProperties extends JsonApi4jProperties {

}
