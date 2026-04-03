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

    private String id;
    private LinksObject links;
    private String status;
    private String code;
    private String title;
    private String detail;
    private ErrorSourceObject source;
    private Object meta;

}
