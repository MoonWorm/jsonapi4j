package pro.api4.jsonapi4j.sampleapp.quarkus.config.oas;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizer;
import pro.api4.jsonapi4j.sampleapp.oas.RateLimitHeadersCustomizer;

public class QuarkusOasCustomizersConfig {

    @Produces
    @Singleton
    OasCustomizer rateLimitHeadersCustomizer() {
        return new RateLimitHeadersCustomizer();
    }

}
