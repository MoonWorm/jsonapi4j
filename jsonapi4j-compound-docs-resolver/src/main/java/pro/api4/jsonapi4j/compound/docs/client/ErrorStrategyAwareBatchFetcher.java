package pro.api4.jsonapi4j.compound.docs.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;

/**
 * Applies the {@link ErrorStrategy} to a fetch: under {@link ErrorStrategy#IGNORE} a failed fetch contributes no
 * resources and is reported as {@link FetchResult#ignoredFailure()}; under {@link ErrorStrategy#FAIL} its failure
 * propagates. Wraps each single request, so one failed chunk of a batch doesn't take its siblings with it.
 */
@Slf4j
public class ErrorStrategyAwareBatchFetcher<R extends DomainSettings> implements BatchFetcher<R> {

    private final BatchFetcher<R> delegate;
    private final ErrorStrategy errorStrategy;

    public ErrorStrategyAwareBatchFetcher(BatchFetcher<R> delegate, ErrorStrategy errorStrategy) {
        this.delegate = Validate.notNull(delegate, "delegate must not be null");
        this.errorStrategy = Validate.notNull(errorStrategy, "errorStrategy must not be null");
    }

    @Override
    public FetchResult fetch(BatchFetch<R> batch, CompoundDocsRequest originalRequest) {
        try {
            return delegate.fetch(batch, originalRequest);
        } catch (ErrorJsonApiResponseException e) {
            if (errorStrategy == ErrorStrategy.IGNORE) {
                log.warn(
                        "Ignoring a failed fetch of '{}' resources {} per error strategy: {}",
                        batch.resourceType(),
                        batch.ids(),
                        e.getMessage()
                );
                return FetchResult.ignoredFailure();
            }
            throw e;
        }
    }

}
