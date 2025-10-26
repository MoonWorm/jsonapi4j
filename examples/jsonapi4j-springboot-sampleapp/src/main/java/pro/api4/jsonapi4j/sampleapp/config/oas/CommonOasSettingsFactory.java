package pro.api4.jsonapi4j.sampleapp.config.oas;

import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.ParameterConfig;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.SecurityConfig;

public final class CommonOasSettingsFactory {

    private CommonOasSettingsFactory() {

    }

    public static SecurityConfig commonSecurityConfig() {
        return SecurityConfig.builder()
                .isClientCredentialsSupported(true)
                .isPkceSupported(true)
                .build();
    }

    public static ParameterConfig.ParameterConfigBuilder idPathParam() {
        return ParameterConfig.builder()
                .name("id")
                .in(OperationOasPlugin.In.PATH)
                .type(OperationOasPlugin.Type.STRING);
    }

}
