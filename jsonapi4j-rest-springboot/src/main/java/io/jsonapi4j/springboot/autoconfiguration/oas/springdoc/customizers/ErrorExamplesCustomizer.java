package io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

public class ErrorExamplesCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        new io.jsonapi4j.oas.customizer.ErrorExamplesCustomizer()
                .customise(openApi);
    }

}
