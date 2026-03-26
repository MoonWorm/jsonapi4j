package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.compound.docs.ErrorStrategy;
import pro.api4.jsonapi4j.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import java.util.Map;

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

    /**
     * Compound docs configurations.
     */
    @Override
    QuarkusCompoundDocsProperties compoundDocs();

    interface QuarkusCompoundDocsProperties extends CompoundDocsProperties {

        /**
         * Enables compound documents filter.
         */
        @WithDefault(CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE)
        @Override
        boolean enabled();

        /**
         * Maximum include traversal depth.
         */
        @WithDefault(CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE)
        @Override
        int maxHops();

        /**
         * Error strategy.
         */
        @WithDefault(CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE)
        @Override
        ErrorStrategy errorStrategy();

        /**
         * Per-resource mapping for downstream URLs.
         */
        @Override
        Map<String, String> mapping();

    }

}
