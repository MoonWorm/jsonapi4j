package pro.api4.jsonapi4j.domain;

/**
 * Represents JSON:API resource type. Resource type is the name of the resource collection.
 * Must be unique across all domains. In the responses - mandatory "type" field (alongside with "id" field) for any
 * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>.
 * <p>
 * Even though this only carries the string representation of the resource type this interface explicitly declares that
 * it's not just a string - it has the semantics of JSON:API resource type. That helps to avoid some mistakes
 * by better guidance of what is expected as a type.
 * <p>
 * This interface can be implemented by Java Enum for better convenience, e.g.:
 *
 * <pre>
 * {@code
 *     public enum MyDomainResources implements ResourceType {
 *
 *         USERS("users"),
 *         COUNTRIES("countries")
 *
 *         private final String resourceType;
 *
 *         public MyDomainResources(String resourceType) {
 *             this.resourceType = resourceType;
 *         }
 *
 *         @Override
 *         public String getType() {
 *              return this.resourceType;
 *         }
 *     }
 * }
 * </pre>
 */
public interface ResourceType {

    /**
     * @return string representation of the JSON:API Resource type
     */
    String getType();

}
