package pro.api4.jsonapi4j.plugin.oas.operation.model;

import lombok.Getter;

@Getter
public enum Type {
    STRING("string"),
    NUMBER("number"),
    INTEGER("integer"),
    BOOLEAN("boolean");

    private final String type;

    Type(String type) {
        this.type = type;
    }

}
