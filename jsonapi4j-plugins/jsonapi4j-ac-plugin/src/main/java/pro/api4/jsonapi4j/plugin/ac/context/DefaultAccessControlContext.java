package pro.api4.jsonapi4j.plugin.ac.context;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.context.PluginVisitorContext;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.Principal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default {@link AccessControlContext}, assembled from the {@link PluginVisitorContext} the access control
 * visitors already hold plus the principal on the current thread.
 */
@EqualsAndHashCode
@ToString
public final class DefaultAccessControlContext implements AccessControlContext {

    private static final Principal ANONYMOUS =
            new AnonymousPrincipal(List.of(), Set.of(), null, Map.of());

    private final Principal principal;
    private final OperationMeta operation;
    private final Object request;
    private final ResourceIdentifierObject resource;
    private final Stage stage;

    private DefaultAccessControlContext(Principal principal,
                                        OperationMeta operation,
                                        Object request,
                                        ResourceIdentifierObject resource,
                                        Stage stage) {
        this.principal = principal;
        this.operation = operation;
        this.request = request;
        this.resource = resource;
        this.stage = stage;
    }

    /**
     * Builds the context for the inbound stage, where no resource has been fetched yet.
     *
     * @param visitorContext the plugin visitor context in scope, may be {@code null}
     * @return the inbound context, never {@code null}
     */
    public static AccessControlContext inbound(PluginVisitorContext<?> visitorContext) {
        return new DefaultAccessControlContext(
                currentPrincipal(),
                visitorContext == null ? null : visitorContext.getOperationMeta(),
                visitorContext == null ? null : visitorContext.getRequest(),
                null,
                Stage.INBOUND
        );
    }

    /**
     * Builds the context for the inbound stage when only the request is in scope.
     *
     * @param request the current request, may be {@code null}
     * @return the inbound context, never {@code null}
     */
    public static AccessControlContext inboundForRequest(Object request) {
        return new DefaultAccessControlContext(currentPrincipal(), null, request, null, Stage.INBOUND);
    }

    /**
     * Builds the context for the outbound stage.
     *
     * @param visitorContext the plugin visitor context in scope, may be {@code null}
     * @param resource       the resource object the requirement is evaluated against, may be {@code null}
     * @return the outbound context, never {@code null}
     */
    public static AccessControlContext outbound(PluginVisitorContext<?> visitorContext,
                                                ResourceIdentifierObject resource) {
        return new DefaultAccessControlContext(
                currentPrincipal(),
                visitorContext == null ? null : visitorContext.getOperationMeta(),
                visitorContext == null ? null : visitorContext.getRequest(),
                resource,
                Stage.OUTBOUND
        );
    }

    /**
     * Builds the context for the outbound stage when only the resource is in scope.
     *
     * @param resource the resource object the requirement is evaluated against, may be {@code null}
     * @return the outbound context, never {@code null}
     */
    public static AccessControlContext outboundForResource(Object resource) {
        return new DefaultAccessControlContext(
                currentPrincipal(),
                null,
                null,
                resource instanceof ResourceIdentifierObject resourceIdentifier ? resourceIdentifier : null,
                Stage.OUTBOUND
        );
    }

    private static Principal currentPrincipal() {
        return AuthenticatedPrincipalContextHolder.getPrincipal().orElse(ANONYMOUS);
    }

    @Override
    public Principal principal() {
        return principal;
    }

    @Override
    public OperationMeta operation() {
        return operation;
    }

    @Override
    public Object request() {
        return request;
    }

    @Override
    public Optional<ResourceIdentifierObject> resource() {
        return Optional.ofNullable(resource);
    }

    @Override
    public Stage stage() {
        return stage;
    }

    private record AnonymousPrincipal(List<String> authenticatedClientEntitlements,
                                      Set<String> authenticatedClientScopes,
                                      String authenticatedUserId,
                                      Map<String, Object> attributes) implements Principal {
    }

}
