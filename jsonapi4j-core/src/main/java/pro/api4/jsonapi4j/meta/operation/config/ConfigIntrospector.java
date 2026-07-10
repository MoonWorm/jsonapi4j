package pro.api4.jsonapi4j.meta.operation.config;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource.ConfigAttributes;

public interface ConfigIntrospector {

    ConfigAttributes config();

    Ref configRef();

}
