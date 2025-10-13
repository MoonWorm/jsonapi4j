package io.jsonapi4j.plugin.ac.annotation;


import io.jsonapi4j.plugin.ac.ownership.NoOpOwnerIdExtractor;
import io.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlOwnership {

    /**
     * Used for the outbound access control evaluation. Ignored for inbound access control evaluation.
     * <p>
     * In case of resource operations the path is evaluated against the current JSON:API Resource root level
     * that has the next fields:
     * <ul>
     *     <li>id</li>
     *     <li>type</li>
     *     <li>attributes</li>
     *     <li>relationships</li>
     *     <li>links</li>
     *     <li>meta</li>
     * </ul>
     * <p/>
     * <p>
     * In case of relationship operations the path is evaluated against the current JSON:API Resource Identifier root
     * level that has the next fields:
     * <ul>
     *     <li>id</li>
     *     <li>type</li>
     *     <li>meta</li>
     * </ul>
     * </p>
     * Default behaviour - disabled. In order to enable it - just specify some path.
     * <p/>
     * Usually it can be set to 'id'. This works for most of the cases except for ones where owner id doesn't
     * match the main resource id.
     * <p/>
     * It's possible to specify the nested field properties e.g. 'attributes.authorId' or similar.
     * <p/>
     * Applicable on JSON:API Resource (Type & Fields), JSON:API Attributes (Type & Fields) levels only.
     *
     * @return field name that holds owner id String value
     */
    String ownerIdFieldPath() default "";

    /**
     * In the most of the cases should be used for inbound access control evaluation where ownership information is
     * extracted from a request. Ignored for outbound access control evaluation.
     * <p>
     * Some operations, e.g. Relationship operations usually don't have information about the owner in the response
     * itself. But usually there might be something in the request.
     * <p/>
     * For example, 'GET /users/2/citizenships'. We can extract owner id from the path.
     */
    Class<? extends OwnerIdExtractor<?>> ownerIdExtractor() default NoOpOwnerIdExtractor.class;

}
