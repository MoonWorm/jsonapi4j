package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'data'
 * scenarios targeting to-one relationship.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *          "data": {
 *              "id": "NO",
 *              "type": "countries"
 *          },
 *          "links": {
 *              "self": "/users/1/relationships/placeOfBirth",
 *              "related": {
 *                  "countries": "/countries?filter[id]=NO"
 *              }
 *          }
 *      }
 *     }
 * </pre>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ToOneRelationshipDoc extends ToOneRelationshipObject {

    public static final String INCLUDED_FIELD = "included";
    public static final String JSONAPI_FIELD = "jsonapi";

    private final List<? extends ResourceObject<?, ?>> included;
    private final JsonApiObject jsonapi;

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links,
                                Object meta,
                                List<? extends ResourceObject<?, ?>> included,
                                JsonApiObject jsonapi) {
        super(data, links, meta);
        this.included = included;
        this.jsonapi = jsonapi;
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links,
                                Object meta,
                                List<? extends ResourceObject<?, ?>> included) {
        this(data, links, meta, included, null);
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links, Object meta) {
        this(data, links, meta, null);
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links) {
        this(data, links, null);
    }

    public ToOneRelationshipDoc(LinksObject links) {
        this(null, links, null);
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data) {
        this(data, null, null);
    }

    public ToOneRelationshipDoc() {
        this(null, null);
    }

}
