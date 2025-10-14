package pro.api4.jsonapi4j.springboot.autoconfiguration;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SpringContextJsonApi4jDomainScanner extends SpringContextScanner {

    private final ObjectProvider<Set<Resource<?, ?>>> resourcesProvider;
    private final ObjectProvider<Set<Relationship<?, ?>>> relationshipsProvider;

    @Autowired
    public SpringContextJsonApi4jDomainScanner(ObjectProvider<Set<Resource<?, ?>>> resourcesProvider,
                                               ObjectProvider<Set<Relationship<?, ?>>> relationshipsProvider) {
        this.resourcesProvider = resourcesProvider;
        this.relationshipsProvider = relationshipsProvider;
    }

    public Set<Resource<?, ?>> getResources() {
        return get(resourcesProvider);
    }

    public Set<Relationship<?, ?>> getRelationships() {
        return get(relationshipsProvider);
    }

}
