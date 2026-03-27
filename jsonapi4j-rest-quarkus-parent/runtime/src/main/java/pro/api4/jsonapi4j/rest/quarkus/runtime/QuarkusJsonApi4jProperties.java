package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;

@Singleton
@ConfigMapping(prefix = "jsonapi4j", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jProperties extends JsonApi4jProperties {

    /**
     * Root path where JsonApi4j servlet is mounted.
     * Example: `/jsonapi`, `/api`.
     */
    @WithDefault(JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH)
    @Override
    String rootPath();


}
