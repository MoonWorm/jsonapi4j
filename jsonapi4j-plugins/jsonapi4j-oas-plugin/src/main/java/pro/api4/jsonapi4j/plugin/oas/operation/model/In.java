package pro.api4.jsonapi4j.plugin.oas.operation.model;

import lombok.Getter;

@Getter
public enum In {
    QUERY("query"),
    PATH("path"),
    HEADER("header");

    private final String name;

    In(String name) {
        this.name = name;
    }

}
