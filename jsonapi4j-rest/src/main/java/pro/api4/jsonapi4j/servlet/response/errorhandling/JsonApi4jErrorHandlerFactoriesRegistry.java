package pro.api4.jsonapi4j.servlet.response.errorhandling;

import java.util.HashMap;
import java.util.Map;

public class JsonApi4jErrorHandlerFactoriesRegistry implements ErrorHandlerFactoriesRegistry {

    private final Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> errorResponseMappers = new HashMap<>();

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return this.errorResponseMappers;
    }

    @Override
    public void register(Class<? extends Throwable> exceptionClass, ErrorsDocSupplier<?> errorsDocSupplier) {
        this.errorResponseMappers.put(exceptionClass, errorsDocSupplier);
    }

}
