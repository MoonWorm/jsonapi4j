package pro.api4.jsonapi4j.springboot.autoconfiguration.oas;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;

@ConfigurationProperties(prefix = "jsonapi4j.oas")
public class SpringOasProperties extends DefaultOasProperties {

}
