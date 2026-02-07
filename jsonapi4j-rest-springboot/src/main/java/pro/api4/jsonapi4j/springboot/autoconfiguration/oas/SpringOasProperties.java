package pro.api4.jsonapi4j.springboot.autoconfiguration.oas;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

@Component
@ConfigurationProperties(prefix = "jsonapi4j.oas")
public class SpringOasProperties extends OasProperties {

}
