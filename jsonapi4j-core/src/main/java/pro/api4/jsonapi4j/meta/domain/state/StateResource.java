package pro.api4.jsonapi4j.meta.domain.state;

import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

@JsonApiResource(resourceType = StateResource.STATE)
public class StateResource implements Resource<StateResource.StateAttributes> {

    public static final String STATE = "state";

    @Override
    public String resolveResourceId(StateAttributes dataSourceDto) {
        return MetaDomain.SINGLETON_ID;
    }

    @Override
    public Object resolveAttributes(StateAttributes a) {
        return a;
    }

    public record StateAttributes(String frameworkVersion,
                                  String javaVersion,
                                  String integration,
                                  int pluginsCount,
                                  int resourcesCount,
                                  int relationshipsCount,
                                  int operationsCount) {
    }
}
