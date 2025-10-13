package io.jsonapi4j.model.document.error;


import io.jsonapi4j.model.document.LinksObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#error-objects">Error Objects</a>
 */
@ToString
@Builder
@EqualsAndHashCode(exclude = {"meta", "title", "detail"})
public class ErrorObject {

    private String id;
    private LinksObject links;
    private String status;
    private String code;
    private String title;
    private String detail;
    private ErrorSourceObject source;
    private Object meta;

    public ErrorObject(String id,
                       LinksObject links,
                       String status,
                       String code,
                       String title,
                       String detail,
                       ErrorSourceObject source,
                       Object meta) {
        this.id = id;
        this.links = links;
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.source = source;
        this.meta = meta;
    }

    public String getId() {
        return id;
    }

    public LinksObject getLinks() {
        return links;
    }

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public ErrorSourceObject getSource() {
        return source;
    }

    public Object getMeta() {
        return meta;
    }
}
