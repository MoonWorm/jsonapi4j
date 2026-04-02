package pro.api4.jsonapi4j.model.document.error;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;

import java.util.List;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for error scenarios.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *          "errors": [
 *              {
 *                  "code": "ARRAY_LENGTH_TOO_LONG",
 *                  "detail": "size must be between 0 and 20",
 *                  "source": {
 *                      "parameter": "countryIds"
 *                  }
 *              }
 *          ]
 *      }
 *     }
 * </pre>
 */
@ToString(of = {"errors", "links"})
@EqualsAndHashCode
public class ErrorsDoc extends BaseDoc {

    public static final String ERRORS_FIELD = "errors";

    private List<ErrorObject> errors;

    public ErrorsDoc(List<ErrorObject> errors,
                     LinksObject links,
                     Object meta,
                     JsonApiObject jsonapi) {
        super(links, meta, jsonapi);
        this.errors = errors;
    }

    public ErrorsDoc(List<ErrorObject> errors,
                     LinksObject links,
                     Object meta) {
        this(errors, links, meta, null);
    }

    public ErrorsDoc(List<ErrorObject> errors,
                     LinksObject links) {
        this(errors, links, null);
    }

    public ErrorsDoc(List<ErrorObject> errors,
                     Object meta) {
        this(errors, null, meta);
    }

    public ErrorsDoc(List<ErrorObject> errors) {
        this(errors, null, null);
    }

    public List<ErrorObject> getErrors() {
        return errors;
    }

}
