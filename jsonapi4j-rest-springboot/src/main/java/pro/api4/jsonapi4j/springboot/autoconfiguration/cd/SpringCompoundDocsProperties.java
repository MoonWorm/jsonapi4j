package pro.api4.jsonapi4j.springboot.autoconfiguration.cd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

@ConfigurationProperties(prefix = "jsonapi4j.cd")
public class SpringCompoundDocsProperties extends DefaultCompoundDocsProperties {

}
