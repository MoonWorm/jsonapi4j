package pro.api4.jsonapi4j.errorhandling;

import java.util.Map;

/**
 * Maps exception types to the JSON:API error documents they are answered with. Applications and plugins contribute
 * their own; the framework assembles them into its error handler registry.
 */
public interface ErrorHandlerFactory {

    Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers();

}
