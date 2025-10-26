package pro.api4.jsonapi4j.sampleapp.domain.user.oas;

import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.ParameterConfig;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.idPathParam;

public final class UserOasSettingsFactory {

    public static final String USER_ID_EXAMPLE = "12345";

    private UserOasSettingsFactory() {

    }

    public static ParameterConfig userIdPathParam() {
        return idPathParam()
                .description("User id")
                .example(USER_ID_EXAMPLE)
                .build();
    }

}
