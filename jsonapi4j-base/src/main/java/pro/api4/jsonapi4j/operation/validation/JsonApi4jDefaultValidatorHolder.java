package pro.api4.jsonapi4j.operation.validation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonApi4jDefaultValidatorHolder {

    public static JsonApi4jDefaultValidator INSTANCE = new JsonApi4jDefaultValidator();

    private JsonApi4jDefaultValidatorHolder() {

    }

}
