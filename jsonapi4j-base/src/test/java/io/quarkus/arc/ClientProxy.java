package io.quarkus.arc;

/**
 * Stand-in for Arc's marker interface, so that proxy unwrapping can be exercised without pulling Quarkus
 * into this module. {@link pro.api4.jsonapi4j.util.ReflectionUtils} matches it by name.
 */
public interface ClientProxy {
}
