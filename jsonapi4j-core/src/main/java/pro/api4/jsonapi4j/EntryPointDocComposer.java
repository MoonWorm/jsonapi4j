package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;
import pro.api4.jsonapi4j.model.document.meta.MetaDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;

import java.util.Collections;
import java.util.Comparator;

/**
 * Composes the JSON:API entry-point document served at {@code GET {rootPath}}.
 * <p>
 * The document links to every resource collection that can be read ({@code READ_MULTIPLE_RESOURCES} is
 * configured), keyed by resource type. Meta API types are not listed one by one: when the meta API is enabled a
 * single {@code meta} link points to {@code /state/this} instead. Links are relative to {@code rootPath}, like every
 * other link the framework generates.
 * <p>
 * An empty top-level {@code meta} is always present because JSON:API requires a top-level document to carry at
 * least one of {@code data}, {@code errors} or {@code meta}.
 * <pre>
 * {
 *   "links": { "self": "/", "users": "/users", "meta": "/state/this" },
 *   "meta": {},
 *   "jsonapi": { "version": "1.1" }
 * }
 * </pre>
 */
public final class EntryPointDocComposer {

    public static final String ENTRY_POINT_PATH = "/";
    public static final String META_LINK = "meta";

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public EntryPointDocComposer(DomainRegistry domainRegistry,
                                 OperationsRegistry operationsRegistry) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
    }

    public MetaDoc compose() {
        LinksObject.LinksObjectBuilder links = LinksObject.builder().self(ENTRY_POINT_PATH);
        domainRegistry.getResourceTypes()
                .stream()
                .filter(resourceType -> !MetaDomain.isMetaResource(resourceType))
                .filter(resourceType -> operationsRegistry.isResourceOperationConfigured(
                        resourceType, OperationType.READ_MULTIPLE_RESOURCES))
                .sorted(Comparator.comparing(ResourceType::getType))
                .forEach(resourceType -> links.href(
                        resourceType.getType(), LinksGenerator.resourcesBasePath(resourceType)));
        if (domainRegistry.isMetaEnabled()) {
            links.href(META_LINK, String.format("/%s/%s", StateResource.STATE, MetaDomain.SINGLETON_ID));
        }
        return new MetaDoc(
                links.build(),
                Collections.emptyMap(),
                new JsonApiObject(JsonApiObject.JSONAPI_VERSION_1_1, null, null)
        );
    }

}
