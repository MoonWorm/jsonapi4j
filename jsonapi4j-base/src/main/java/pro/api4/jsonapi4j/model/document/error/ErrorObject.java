package pro.api4.jsonapi4j.model.document.error;


import lombok.*;
import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#error-objects">Error Objects</a>
 */
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@Getter
public class ErrorObject {

    public static final String ID_FIELD = "id";
    public static final String LINKS_FIELD = "links";
    public static final String STATUS_FIELD = "status";
    public static final String CODE_FIELD = "code";
    public static final String TITLE_FIELD = "title";
    public static final String DETAIL_FIELD = "detail";
    public static final String SOURCE_FIELD = "source";
    public static final String META_FIELD = "meta";

    private String id;
    private LinksObject links;
    private String status;
    private String code;
    private String title;
    private String detail;
    private ErrorSourceObject source;
    private Object meta;

}
