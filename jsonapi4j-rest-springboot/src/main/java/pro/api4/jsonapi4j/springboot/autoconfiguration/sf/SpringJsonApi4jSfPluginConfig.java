package pro.api4.jsonapi4j.springboot.autoconfiguration.sf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;

@ConditionalOnProperty(
        prefix = "jsonapi4j.sf",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = SpringJsonApi4jSfPluginConfig.class)
@Configuration
public class SpringJsonApi4jSfPluginConfig {

    @Bean
    public JsonApiSparseFieldsetsPlugin jsonApiSparseFieldsetsPlugin(SfProperties sfProperties) {
        return new JsonApiSparseFieldsetsPlugin(sfProperties);
    }

}
