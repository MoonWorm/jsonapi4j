package io.jsonapi4j.domain;

/**
 * Represents JSON:API relationship name.
 * Must be unique for the given parent resource, but recommended to be unique across all domains.
 * In the responses - part of the
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationships Object</a>.
 * <p>
 * Even though this only carries the name of the relationship this interface explicitly shows that it's
 * not just a string - it has the semantics of JSON:API relationship name. That helps to avoid some mistakes
 * by better guidance of what is expected as a type.
 * <p>
 * This interface can be implemented by Java Enum for better convenience, e.g.:
 *
 * <pre>
 * {@code
 *     public enum MyDomainRelationships implements RelationshipName {
 *
 *         CITIZENSHIPS("citizenships"),
 *         CURRENCIES("currencies")
 *
 *         private final String relationshipName;
 *
 *         public MyDomainRelationships(String relationshipName) {
 *             this.relationshipName = relationshipName;
 *         }
 *
 *         @Override
 *         public String getName() {
 *              return this.relationshipName;
 *         }
 *     }
 * }
 * </pre>
 */
public interface RelationshipName {

    /**
     * @return string representation for the relationship name of the given JSON:API relationship
     */
    String getName();

}
