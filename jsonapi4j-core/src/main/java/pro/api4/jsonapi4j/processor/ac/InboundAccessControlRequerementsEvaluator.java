package pro.api4.jsonapi4j.processor.ac;

import lombok.AllArgsConstructor;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@AllArgsConstructor
@Data
public class InboundAccessControlRequerementsEvaluator {

    private static final Logger log = LoggerFactory.getLogger(InboundAccessControlRequerementsEvaluator.class);

    private final AccessControlEvaluator accessControlEvaluator;
    private InboundAccessControlSettings inboundAccessControlSettings;

    public void calculateEffectiveAccessControlSettings(Class<?> requestClass) {
        InboundAccessControlSettings fromAnnotation
                = InboundAccessControlSettings.fromAnnotation(requestClass);
        this.inboundAccessControlSettings = InboundAccessControlSettings.merge(
                this.inboundAccessControlSettings,
                fromAnnotation
        );
    }

    public <REQUEST, DATA> DATA retrieveDataAndEvaluateInboundAcReq(REQUEST request,
                                                                    Supplier<DATA> dataSupplier) {
        if (accessControlEvaluator.evaluateInboundRequirements(
                request,
                inboundAccessControlSettings.getForRequest())
        ) {
            log.info("Access is allowed on a request level. Proceeding...");
            return dataSupplier.get();
        } else {
            log.info("Access is not allowed on a request level, returning empty response");
            return null;
        }
    }

}
