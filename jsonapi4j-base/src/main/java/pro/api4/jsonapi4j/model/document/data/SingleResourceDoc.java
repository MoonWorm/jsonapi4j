package pro.api4.jsonapi4j.model.document.data;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'data'
 * scenarios targeting a single primary resource.
 * <p>
 * <pre>
 *     {@code
 *     {
 *          "data": {
 *              "attributes": {
 *                  "firstName": "John",
 *                  "lastName": "Doe",
 *                  "email": "john@doe.com"
 *              },
 *              "relationships": {
 *                  "citizenships": {
 *                      "links": {
 *                          "self": "/users/1/relationships/citizenships"
 *                      }
 *                  }
 *              },
 *              "links": {
 *                  "self": "/users/1"
 *              },
 *              "id": "1",
 *              "type": "users"
 *          }
 *      }
 *     }
 * </pre>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SingleResourceDoc<PRIMARY_RESOURCE extends ResourceObject<?, ?>>
        extends AbstractSingleDataItemDoc<PRIMARY_RESOURCE> {

    public SingleResourceDoc(PRIMARY_RESOURCE data,
                             LinksObject links,
                             Object meta,
                             List<? extends ResourceObject<?, ?>> included,
                             JsonApiObject jsonapi) {
        super(data, links, meta, included, jsonapi);
    }

    public SingleResourceDoc(PRIMARY_RESOURCE data,
                             LinksObject links,
                             Object meta,
                             List<? extends ResourceObject<?, ?>> included) {
        this(data, links, meta, included, null);
    }

    public SingleResourceDoc(PRIMARY_RESOURCE data,
                             LinksObject links,
                             Object meta) {
        this(data, links, meta, null);
    }

    public SingleResourceDoc(PRIMARY_RESOURCE data,
                             LinksObject links) {
        this(data, links, null);
    }

    public SingleResourceDoc(PRIMARY_RESOURCE data) {
        this(data, null, null);
    }

    public SingleResourceDoc(PRIMARY_RESOURCE data,
                             Object meta) {
        this(data, null, meta);
    }

    public SingleResourceDoc() {
        this(null);
    }

}
