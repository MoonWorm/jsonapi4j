package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;

@Component
@ConfigurationProperties(prefix = "jsonapi4j")
public class SpringJsonApi4JProperties extends DefaultJsonApi4jProperties {

    // Spring Boot expects 'getter' while CompoundDocsProperties expose 'compoundDocs'
    public CompoundDocsProperties getCompoundDocs() {
        return super.compoundDocs();
    }

}
