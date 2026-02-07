package pro.api4.jsonapi4j.springboot.autoconfiguration.ac;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

@ConditionalOnClass(value = JsonApiAccessControlPlugin.class)
@Configuration
public class SpringJsonApi4jAcPluginConfig {

    @Bean
    public JsonApiAccessControlPlugin jsonApiAccessControlPlugin(AccessControlEvaluator accessControlEvaluator) {
        return new JsonApiAccessControlPlugin(accessControlEvaluator);
    }

    @Bean
    public AccessControlEvaluator jsonapi4jAccessControlEvaluator(
            AccessTierRegistry accessTierRegistry
    ) {
        return new DefaultAccessControlEvaluator(accessTierRegistry);
    }

}
