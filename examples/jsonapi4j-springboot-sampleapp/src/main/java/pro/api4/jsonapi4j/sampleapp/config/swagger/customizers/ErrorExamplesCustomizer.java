package pro.api4.jsonapi4j.sampleapp.config.swagger.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

public class ErrorExamplesCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.plugin.oas.customizer.ErrorExamplesCustomizer()
                .customise(openApi);
    }

}
