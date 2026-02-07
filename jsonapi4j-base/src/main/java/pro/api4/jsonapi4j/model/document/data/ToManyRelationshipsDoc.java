package pro.api4.jsonapi4j.model.document.data;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ToManyRelationshipsDoc extends AbstractMultipleDataItemsDoc<ResourceIdentifierObject> {

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links,
                                  Object meta
    ) {
        super(data, meta, links);
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data,
                                  LinksObject links
    ) {
        super(data, null, links);
    }

    public ToManyRelationshipsDoc(LinksObject links) {
        super(null, null, links);
    }

    public ToManyRelationshipsDoc(List<ResourceIdentifierObject> data) {
        this(data, null);
    }

    public ToManyRelationshipsDoc() {
        this(null, null);
    }

    public static ToManyRelationshipsDoc fromBaseDoc(List<ResourceIdentifierObject> data,
                                                     BaseDoc base) {
        return new ToManyRelationshipsDoc(data, base.getLinks(), base.getMeta());
    }

}
