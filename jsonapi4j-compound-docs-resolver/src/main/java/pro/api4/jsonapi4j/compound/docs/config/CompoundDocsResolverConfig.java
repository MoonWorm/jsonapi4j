package pro.api4.jsonapi4j.compound.docs.config;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CompoundDocsResolverConfig {

    private final boolean enabled;
    private final int maxHops;
    private final UnsupportedIncludeStrategy unsupportedIncludes;
    private final int maxIncludedResources;
    private final ErrorStrategy errorStrategy;
    private final List<Propagation> propagation;
    /**
     * Headers carrying the client's identity - sent only to domains trusted with it, see
     * {@link pro.api4.jsonapi4j.compound.docs.DomainSettings.OverHttp#propagateCredentials()}. Matched ignoring case.
     */
    private final Set<String> credentialHeaders;
    private final Deduplication deduplication;
    private final long httpConnectTimeoutMs;
    private final long httpTotalTimeoutMs;
    private final boolean cacheEnabled;
    private final int cacheMaxSize;

}
