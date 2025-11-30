package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.operation.Operation;

import java.util.Set;

@Component
public class SpringContextJsonApi4jOperationsScanner extends SpringContextScanner {

    private final ObjectProvider<Set<Operation>> operationsProvider;

    public SpringContextJsonApi4jOperationsScanner(ObjectProvider<Set<Operation>> operationsProvider) {
        this.operationsProvider = operationsProvider;
    }

    public Set<Operation> getOperations() {
        return get(operationsProvider);
    }

}
