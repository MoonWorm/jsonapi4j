package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.operation.ResourceOperation;

import java.util.Set;

@Component
public class SpringContextJsonApi4jOperationsScanner extends SpringContextScanner {

    private final ObjectProvider<Set<ResourceOperation>> operationsProvider;

    public SpringContextJsonApi4jOperationsScanner(ObjectProvider<Set<ResourceOperation>> operationsProvider) {
        this.operationsProvider = operationsProvider;
    }

    public Set<ResourceOperation> getOperations() {
        return get(operationsProvider);
    }

}
