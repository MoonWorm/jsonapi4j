package pro.api4.jsonapi4j.http;

public enum HttpHeaders {

    ACCEPT("Accept"),
    CONTENT_TYPE("Content-Type"),
    CACHE_CONTROL("Cache-Control");

    private String name;

    HttpHeaders(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
