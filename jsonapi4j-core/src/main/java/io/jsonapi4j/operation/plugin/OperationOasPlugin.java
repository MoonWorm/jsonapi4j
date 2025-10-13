package io.jsonapi4j.operation.plugin;

import io.jsonapi4j.plugin.OperationPlugin;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
public class OperationOasPlugin implements OperationPlugin<OperationOasPlugin> {

    public static final OperationOasPlugin DEFAULT = OperationOasPlugin.builder().build();

    @Builder.Default
    private String resourceNameSingle = "";
    @Builder.Default
    private String resourceNamePlural = "";
    @Builder.Default
    private SecurityConfig securityConfig = SecurityConfig.builder().build();
    @Builder.Default
    private List<ParameterConfig> parameters = Collections.emptyList();
    @Builder.Default
    private Class<?> payloadType = null;

    @Override
    public Class<OperationOasPlugin> getPluginClass() {
        return OperationOasPlugin.class;
    }

    @Getter
    public enum In {
        QUERY("query"), PATH("path"), HEADER("header");

        private final String name;

        In(String name) {
            this.name = name;
        }

    }

    @Getter
    public enum Type {
        STRING("string"), NUMBER("number"), INTEGER("integer"), BOOLEAN("boolean");

        private String type;

        Type(String type) {
            this.type = type;
        }

    }

    @Builder
    @Data
    public static class SecurityConfig {
        @Builder.Default
        private boolean isClientCredentialsSupported = false;
        @Builder.Default
        private boolean isPkceSupported = false;
        @Builder.Default
        private List<String> requiredScopes = Collections.emptyList();
    }

    @EqualsAndHashCode(of = "name")
    @Builder
    @Data
    public static class ParameterConfig {
        private String name;
        private In in;
        private String description;
        private String example;
        @Builder.Default
        private boolean isRequired = true;
        @Builder.Default
        private boolean isArray = false;
        @Builder.Default
        private Type type = Type.STRING;
    }

}
