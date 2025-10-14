package pro.api4.jsonapi4j.springboot.autoconfiguration;

import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SpringContextJsonApi4jErrorHandlerFactoriesScanner extends SpringContextScanner {

    private final ObjectProvider<Set<ErrorHandlerFactory>> errorHandlerFactoriesProvider;

    @Autowired
    public SpringContextJsonApi4jErrorHandlerFactoriesScanner(ObjectProvider<Set<ErrorHandlerFactory>> errorHandlerFactoriesProvider) {
        this.errorHandlerFactoriesProvider = errorHandlerFactoriesProvider;
    }

    public Set<ErrorHandlerFactory> getErrorHandlerFactories() {
        return get(this.errorHandlerFactoriesProvider);
    }

}
