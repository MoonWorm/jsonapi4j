package pro.api4.jsonapi4j.springboot.autoconfiguration.ac;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.ac.config.AcProperties;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

@ConditionalOnProperty(
        prefix = "jsonapi4j.ac",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = {JsonApiAccessControlPlugin.class})
@EnableConfigurationProperties(SpringAcProperties.class)
@Configuration
public class SpringJsonApi4jAcPluginConfig {

    @Bean
    public JsonApiAccessControlPlugin jsonApiAccessControlPlugin(AccessControlEvaluator accessControlEvaluator,
                                                                 AcProperties acProperties) {
        return new JsonApiAccessControlPlugin(accessControlEvaluator, acProperties);
    }

    @ConditionalOnMissingBean(AccessControlEvaluator.class)
    @Bean
    public AccessControlEvaluator jsonapi4jAccessControlEvaluator(
            AccessTierRegistry accessTierRegistry
    ) {
        return new DefaultAccessControlEvaluator(accessTierRegistry);
    }

}
