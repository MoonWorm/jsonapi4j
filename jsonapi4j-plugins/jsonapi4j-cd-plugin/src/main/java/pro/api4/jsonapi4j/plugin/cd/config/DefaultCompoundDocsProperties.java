package pro.api4.jsonapi4j.plugin.cd.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultCompoundDocsProperties implements CompoundDocsProperties {

    private boolean enabled = Boolean.parseBoolean(CD_ENABLED_DEFAULT_VALUE);
    private int maxHops = Integer.parseInt(CD_MAX_HOPS_DEFAULT_VALUE);
    private ErrorStrategy errorStrategy = ErrorStrategy.valueOf(CD_ERROR_STRATEGY_DEFAULT_VALUE);
    private Map<String, String> mapping = Collections.emptyMap();
    private List<Propagation> propagation = parsePropagationString(CD_PROPAGATION_DEFAULT_VALUE);

    public static CompoundDocsProperties toCdProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        Object cdPropertiesObject = jsonApi4jPropertiesRaw.get(CompoundDocsProperties.CD_PROPERTY_NAME);
        Map<String, Object> cdPropertiesRaw = Collections.emptyMap();
        if (cdPropertiesObject instanceof Map cdPropertiesMap) {
            //noinspection unchecked
            cdPropertiesRaw = cdPropertiesMap;
        }
        CompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
        if (!cdPropertiesRaw.isEmpty()) {
            cdProperties = JsonApi4jConfigReader.convertToConfig(
                    cdPropertiesRaw,
                    DefaultCompoundDocsProperties.class
            );
        }
        return cdProperties;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public int maxHops() {
        return maxHops;
    }

    @Override
    public ErrorStrategy errorStrategy() {
        return errorStrategy;
    }

    @Override
    public Map<String, String> mapping() {
        return mapping;
    }

    @Override
    public List<Propagation> propagation() {
        return propagation;
    }

}
