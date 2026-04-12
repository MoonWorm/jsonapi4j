package pro.api4.jsonapi4j.springboot.autoconfiguration.cd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

@Component
@ConfigurationProperties(prefix = "jsonapi4j.cd")
public class SpringCompoundDocsProperties extends DefaultCompoundDocsProperties {

}
