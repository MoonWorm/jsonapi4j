package pro.api4.jsonapi4j.sampleapp.config.oas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizer;
import pro.api4.jsonapi4j.sampleapp.oas.RateLimitHeadersCustomizer;

@Configuration
public class SpringOasCustomizersConfig {

    @Bean
    public OasCustomizer rateLimitHeadersCustomizer() {
        return new RateLimitHeadersCustomizer();
    }

}
