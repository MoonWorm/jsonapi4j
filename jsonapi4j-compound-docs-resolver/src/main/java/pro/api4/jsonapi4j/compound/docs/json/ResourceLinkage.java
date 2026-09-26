package pro.api4.jsonapi4j.compound.docs.json;

import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Map;
import java.util.Set;

/**
 * A resource object reduced to what compound docs traversal needs: its identity and, per relationship name, the
 * resources its linkage points to.
 *
 * @param idAndType     identity of the resource, or {@code null} when the resource object has no textual
 *                      {@code type}/{@code id}
 * @param relationships relationship name to the resources referenced by its {@code data} member
 */
public record ResourceLinkage(IdAndType idAndType, Map<String, Set<IdAndType>> relationships) {
}
