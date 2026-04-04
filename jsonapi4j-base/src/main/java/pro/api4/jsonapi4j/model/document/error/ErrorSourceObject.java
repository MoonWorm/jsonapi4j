package pro.api4.jsonapi4j.model.document.error;

import lombok.*;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#error-objects">Error Source Object</a>
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
public class ErrorSourceObject {

    private String pointer;
    private String parameter;
    private String header;

}
