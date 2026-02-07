package pro.api4.jsonapi4j.model.document.error;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
public class ErrorsDoc {

    private List<ErrorObject> errors;
    private LinksObject links;
    private Object meta;

    public ErrorsDoc(List<ErrorObject> errors,
                     LinksObject links,
                     Object meta) {
        this.errors = errors;
        this.links = links;
        this.meta = meta;
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

    public LinksObject getLinks() {
        return links;
    }

    public Object getMeta() {
        return meta;
    }
}
