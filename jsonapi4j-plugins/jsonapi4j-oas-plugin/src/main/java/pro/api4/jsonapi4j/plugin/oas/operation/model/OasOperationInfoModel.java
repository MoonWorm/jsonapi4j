package pro.api4.jsonapi4j.plugin.oas.operation.model;

import lombok.*;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class OasOperationInfoModel {

    @Builder.Default
    private String summary = "";
    @Builder.Default
    private String description = "";
    @Builder.Default
    private List<String> sortableFields = Collections.emptyList();
    @Builder.Default
    private Set<PaginationStyle> paginationStyles = EnumSet.of(PaginationStyle.CURSOR);
    @Builder.Default
    private SecurityConfig securityConfig = SecurityConfig.builder().build();
    @Builder.Default
    private List<Parameter> parameters = Collections.emptyList();
    @Builder.Default
    private Class<?> payloadType = NotApplicable.class;

    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class SecurityConfig {
        @Builder.Default
        private boolean clientCredentialsSupported = false;
        @Builder.Default
        private boolean pkceSupported = false;
        @Builder.Default
        private List<String> requiredScopes = Collections.emptyList();
    }

    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class Parameter {
        private final String name;
        @Builder.Default
        private In in = In.QUERY;
        @Builder.Default
        private String description = "";
        @Builder.Default
        private String example = "";
        @Builder.Default
        private boolean required = true;
        @Builder.Default
        private boolean array = false;
        @Builder.Default
        private Type type = Type.STRING;
    }

    private static Set<PaginationStyle> toOrderedStyles(PaginationStyle[] styles) {
        return styles.length == 0
                ? EnumSet.noneOf(PaginationStyle.class)
                : EnumSet.copyOf(Arrays.asList(styles));
    }

    public static OasOperationInfoModel fromAnnotation(OasOperationInfo oasOperationInfo) {
        if (oasOperationInfo == null) {
            return null;
        }
        return OasOperationInfoModel.builder()
                .summary(oasOperationInfo.summary())
                .description(oasOperationInfo.description())
                .securityConfig(
                        SecurityConfig.builder()
                                .clientCredentialsSupported(oasOperationInfo.securityConfig().clientCredentialsSupported())
                                .pkceSupported(oasOperationInfo.securityConfig().pkceSupported())
                                .requiredScopes(Arrays.asList(oasOperationInfo.securityConfig().requiredScopes()))
                                .build()
                )
                .parameters(
                        Arrays.stream(oasOperationInfo.parameters())
                                .map(p -> Parameter.builder()
                                        .name(p.name())
                                        .in(p.in())
                                        .description(p.description())
                                        .example(p.example())
                                        .required(p.required())
                                        .array(p.array())
                                        .type(p.type())
                                        .build())
                                .toList()
                )
                .sortableFields(Arrays.asList(oasOperationInfo.sortableFields()))
                .paginationStyles(toOrderedStyles(oasOperationInfo.pagination()))
                .payloadType(oasOperationInfo.payloadType())
                .build();
    }

}
