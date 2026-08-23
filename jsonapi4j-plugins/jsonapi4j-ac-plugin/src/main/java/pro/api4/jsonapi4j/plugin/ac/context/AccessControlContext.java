package pro.api4.jsonapi4j.plugin.ac.context;

import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.principal.Principal;

import java.util.Optional;

/**
 * Everything an access control requirement is decided against, for one evaluation.
 * <p>
 * The four accessors follow the vocabulary shared by XACML, AWS Cedar and AWS IAM — who is asking, what they
 * are trying to do, what they are trying to do it to, and the surrounding circumstances:
 * <ul>
 *     <li>{@link #principal()} — the subject, including its {@code attributes()}</li>
 *     <li>{@link #operation()} — the action: operation type, resource type, relationship name</li>
 *     <li>{@link #resource()} — the object being guarded, once one exists</li>
 *     <li>{@link #request()} — the environment: headers, filters, includes</li>
 * </ul>
 * <p>
 * A context is read-only and lives for a single evaluation. It is handed to
 * {@link pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy} implementations, so it deliberately exposes only
 * what a policy needs to decide — not the pipeline internals carried by
 * {@link pro.api4.jsonapi4j.plugin.context.PluginVisitorContext}, which is what it is built from.
 *
 * @see pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy
 */
public interface AccessControlContext {

    /**
     * Returns the authenticated caller.
     * <p>
     * Never {@code null}: an anonymous request yields a principal whose fields are {@code null} or empty,
     * so a policy can read {@code attributes()} without a null check.
     *
     * @return the caller, never {@code null}
     */
    Principal principal();

    /**
     * Returns metadata about the operation being performed — its type, the resource type it targets, and the
     * relationship name when the operation is a relationship one.
     *
     * @return the operation being performed, or {@code null} when evaluation happens outside an operation
     */
    OperationMeta operation();

    /**
     * Returns the incoming request. Typed as {@code Object} because the request type is a framework-level
     * generic; cast it to {@code JsonApiRequest} in the servlet stack.
     *
     * @return the current request, or {@code null} when none is in scope
     */
    Object request();

    /**
     * Returns the JSON:API resource object being emitted, present only at {@link Stage#OUTBOUND}.
     * <p>
     * This is the top-level resource the requirement is evaluated against, not the nested object currently
     * being anonymized — the same object {@code ownerIdFieldPath} resolves against. For a relationship it is the resource identifier object.
     *
     * @return the resource being guarded, empty inbound
     */
    Optional<ResourceIdentifierObject> resource();

    /**
     * Returns the guarded resource narrowed to the given type, or empty when there is no resource or it is
     * something else — a checked narrowing that never throws:
     * <pre>{@code
     * context.resource(ResourceObject.class)
     *        .map(ResourceObject::getAttributes)
     *        .filter(OrderAttributes.class::isInstance)
     *        ...
     * }</pre>
     * <p>
     * Note this narrows the <em>resource</em>, not its payload: {@code ResourceObject} is generic in its
     * attributes and relationships types, and those parameters cannot be recovered here — a policy is
     * referenced from an annotation as a bare {@code Class}, so there is nothing to bind them to. Reach the
     * attributes with a second {@code instanceof} against your own attributes class.
     *
     * @param type          the resource type to narrow to
     * @param <T>           the resource type to narrow to
     * @return the resource when it is of the given type, empty otherwise
     */
    default <T extends ResourceIdentifierObject> Optional<T> resource(Class<T> type) {
        return resource().filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns the lifecycle point at which this evaluation is happening.
     *
     * @return the evaluation stage, never {@code null}
     */
    Stage stage();

}
