package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class CompoundDocsResolverConfig {

    private final ObjectMapper objectMapper;
    private final DomainUrlResolver domainUrlResolver;
    private final ExecutorService executorService;
    private final int maxHops;
    private final ErrorStrategy errorStrategy;

    public CompoundDocsResolverConfig(ObjectMapper objectMapper,
                                      DomainUrlResolver domainUrlResolver,
                                      ExecutorService executorService,
                                      int maxHops,
                                      ErrorStrategy errorStrategy) {
        this.objectMapper = objectMapper;
        this.domainUrlResolver = domainUrlResolver;
        this.executorService = executorService;
        this.maxHops = maxHops;
        this.errorStrategy = errorStrategy;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public DomainUrlResolver getDomainUrlResolver() {
        return domainUrlResolver;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public int getMaxHops() {
        return maxHops;
    }

    public ErrorStrategy getErrorStrategy() {
        return errorStrategy;
    }

    public CompoundDocsResolverConfig validate() {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper is not configured");
        }
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService is not configured");
        }
        if (domainUrlResolver == null) {
            throw new IllegalArgumentException("DomainUrlResolver is not configured");
        }
        return this;
    }

    public interface DomainUrlResolver {
        URI getDomainUrl(String resourceType);
    }

    public static class DefaultDomainUrlResolver implements DomainUrlResolver {

        private final Map<String, URI> mappings;
        private URI defaultDomainUrl = URI.create("http://localhost:8080");

        public DefaultDomainUrlResolver(Map<String, URI> mappings) {
            this.mappings = mappings;
        }

        public DefaultDomainUrlResolver(Map<String, URI> mappings,
                                        URI defaultDomainUrl) {
            this.mappings = mappings;
            this.defaultDomainUrl = defaultDomainUrl;
        }

        @Override
        public URI getDomainUrl(String resourceType) {
            URI result = mappings.get(resourceType);
            if (result != null) {
                return result;
            }
            return defaultDomainUrl;
        }
    }

    public enum ErrorStrategy {
        FAIL, IGNORE
    }
}
