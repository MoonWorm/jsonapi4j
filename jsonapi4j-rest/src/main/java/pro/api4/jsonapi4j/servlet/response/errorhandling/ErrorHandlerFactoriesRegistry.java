package pro.api4.jsonapi4j.servlet.response.errorhandling;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import java.util.Map;

public interface ErrorHandlerFactoriesRegistry {

    ErrorsDocSupplier<Throwable> INTERNAL_SERVER_ERROR_MAPPER = new ErrorsDocSupplier<>() {
        @Override
        public ErrorsDoc getErrorResponse(Throwable e) {
            return ErrorsDocFactory.internalServerErrorsDoc();
        }

        @Override
        public int getHttpStatus(Throwable e) {
            return 500;
        }
    };

    void register(Class<? extends Throwable> exceptionClass,
                  ErrorsDocSupplier<?> errorsDocSupplier);

    Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers();

    @SuppressWarnings("unchecked")
    default <T extends Throwable> ErrorsDocSupplier<T> getErrorResponseMapper(Class<T> exceptionClass) {
        ErrorsDocSupplier<T> mapper = (ErrorsDocSupplier<T>) getErrorResponseMappers().get(exceptionClass);
        if (mapper == null) {
            return getErrorResponseMappers().keySet()
                    .stream()
                    .filter(clazz -> clazz.isAssignableFrom(exceptionClass))
                    .findFirst()
                    .map(clazz -> (ErrorsDocSupplier<T>) getErrorResponseMappers().get(clazz))
                    .orElse((ErrorsDocSupplier<T>) INTERNAL_SERVER_ERROR_MAPPER);
        }
        return mapper;
    }

    @SuppressWarnings("unchecked")
    default <T extends Throwable> ErrorsDocSupplier<T> getErrorResponseMapper(T throwable) {
        return getErrorResponseMapper((Class<T>) throwable.getClass());
    }

    default void registerAll(Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> errorResponseMappers) {
        errorResponseMappers.forEach(this::register);
    }

    default void registerAll(ErrorHandlerFactory errorHandlerFactory) {
        registerAll(errorHandlerFactory.getErrorResponseMappers());
    }

    default <T extends Throwable> ErrorsDoc resolveErrorsDoc(T throwable) {
        return getErrorResponseMapper(throwable).getErrorResponse(throwable);
    }

    default <T extends Throwable> int resolveStatusCode(T throwable) {
        return getErrorResponseMapper(throwable).getHttpStatus(throwable);
    }

}
