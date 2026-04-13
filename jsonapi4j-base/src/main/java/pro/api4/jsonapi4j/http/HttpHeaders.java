package pro.api4.jsonapi4j.http;

public enum HttpHeaders {

    ACCEPT("Accept"),
    CONTENT_TYPE("Content-Type"),
    CACHE_CONTROL("Cache-Control"),
    X_DISABLE_COMPOUND_DOCS("x-disable-compound-docs");

    private String name;

    HttpHeaders(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
