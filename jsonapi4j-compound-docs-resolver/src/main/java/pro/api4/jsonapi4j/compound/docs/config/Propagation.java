package pro.api4.jsonapi4j.compound.docs.config;

/**
 * These fields don't make sense to propagate:
 * <ul>
 *     <li><code>filter[*]</code> param only makes sense for the main request, later it's used for Compound Docs resolution</li>
 *     <li><code>sort</code> only make sense for the main request, all related resources shouldn't follow any specific 'sort' logic</li>
 *     <li><code>include</code> doesn't make sense to propagate because it initiates compound docs resolution at first place</li>
 * </ul>
 *
 */
public enum Propagation {
    FIELDS, CUSTOM_QUERY_PARAMS, HEADERS
}
