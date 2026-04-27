package pro.api4.jsonapi4j.operation.validation;

public final class JsonApi4jDefaultValidatorHolder {

    public static JsonApi4jDefaultValidator INSTANCE = new JsonApi4jDefaultValidator();

    private JsonApi4jDefaultValidatorHolder() {

    }

    public static void configure(ValidationProperties properties) {
        INSTANCE = new JsonApi4jDefaultValidator(properties);
    }

}
