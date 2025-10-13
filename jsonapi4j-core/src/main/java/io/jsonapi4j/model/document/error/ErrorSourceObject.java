package io.jsonapi4j.model.document.error;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#error-objects">Error Source Object</a>
 */
@ToString
@EqualsAndHashCode
@Builder
public class ErrorSourceObject {

    private String pointer;
    private String parameter;
    private String header;

    public ErrorSourceObject(String pointer,
                             String parameter,
                             String header) {
        this.pointer = pointer;
        this.parameter = parameter;
        this.header = header;
    }

    public String getPointer() {
        return pointer;
    }

    public String getParameter() {
        return parameter;
    }

    public String getHeader() {
        return header;
    }

}
