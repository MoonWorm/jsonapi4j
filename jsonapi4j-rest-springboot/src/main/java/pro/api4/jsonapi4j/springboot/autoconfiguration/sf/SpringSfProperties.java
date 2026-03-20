package pro.api4.jsonapi4j.springboot.autoconfiguration.sf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.plugin.sf.config.DefaultSfProperties;

@Component
@ConfigurationProperties(prefix = "jsonapi4j.sf")
public class SpringSfProperties extends DefaultSfProperties {
}
