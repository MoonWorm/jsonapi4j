package pro.api4.jsonapi4j.meta.operation.config;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource.ConfigAttributes;

public interface ConfigIntrospector {

    ConfigAttributes config();

    ResourceRef configRef();

}
