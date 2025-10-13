package io.jsonapi4j.servlet.response.errorhandling;

import java.util.Map;

public interface ErrorHandlerFactory {

    Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers();

}
