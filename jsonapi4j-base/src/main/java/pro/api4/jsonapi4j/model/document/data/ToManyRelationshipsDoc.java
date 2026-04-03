package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'data'
 * scenarios targeting to-many relationships.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *          "data": [
 *              {
 *                  "id": "NO",
 *                  "type": "countries"
 *              },
 *              {
 *                  "id": "FI",
 *                  "type": "countries"
 *              }
 *          ],
 *          "links": {
 *              "self": "/users/1/relationships/citizenships",
 *              "related": {
 *                  "countries": "/countries?filter[id]=FI,NO"
 *              },
 *              "next": "/users/1/relationships/citizenships?page%5Bcursor%5D=DoJu"
 *          }
 *      }
 *     }
 * </pre>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ToManyRelationshipsDoc extends ToManyRelationshipObject {

    public static final String INCLUDED_FIELD = "included";
    public static final String JSONAPI_FIELD = "jsonapi";

    private final List<? extends ResourceObject<?, ?>> included;
    private final JsonApiObject jsonapi;

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links,
                                  Object meta,
                                  List<? extends ResourceObject<?, ?>> included,
                                  JsonApiObject jsonapi
    ) {
        super(data, links, meta);
        this.included = included;
        this.jsonapi = jsonapi;
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links,
                                  Object meta,
                                  List<? extends ResourceObject<?, ?>> included
    ) {
        this(data, links, meta, included, null);
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links,
                                  Object meta
    ) {
        this(data, links, meta, null);
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links
    ) {
        this(data, links, null);
    }

    public ToManyRelationshipsDoc(LinksObject links) {
        this(null, links);
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data) {
        this(data, null);
    }

    public ToManyRelationshipsDoc() {
        this(null, null);
    }

}
