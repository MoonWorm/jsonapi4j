package io.jsonapi4j.springboot.autoconfiguration;

import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Set;

public abstract class SpringContextScanner {

    protected <T> Set<T> get(ObjectProvider<Set<T>> op) {
        Set<T> result = op.getIfAvailable();
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

}
