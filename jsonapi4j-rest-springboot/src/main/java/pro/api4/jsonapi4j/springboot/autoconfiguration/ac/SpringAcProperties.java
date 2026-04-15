package pro.api4.jsonapi4j.springboot.autoconfiguration.ac;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pro.api4.jsonapi4j.plugin.ac.config.DefaultAcProperties;

@ConfigurationProperties(prefix = "jsonapi4j.ac")
public class SpringAcProperties extends DefaultAcProperties {
}
