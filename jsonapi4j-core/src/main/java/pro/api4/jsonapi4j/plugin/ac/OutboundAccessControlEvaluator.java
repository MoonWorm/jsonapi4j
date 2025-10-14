package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface OutboundAccessControlEvaluator {

    boolean evaluateOutboundRequirements(
            Object ownerIdHolder,
            AccessControlRequirements accessControlRequirements
    );

    default <TYPE> Set<String> anonymizeFieldValues(
            TYPE object,
            Object ownerIdHolder,
            Map<String, AccessControlRequirements> fieldLevelAcSettings
    ) {
        Set<String> anonymizedFields = new HashSet<>();
        if (fieldLevelAcSettings != null) {
            for (Map.Entry<String, AccessControlRequirements> e : fieldLevelAcSettings.entrySet()) {
                String fieldName = e.getKey();
                AccessControlRequirements fieldAcInfo = e.getValue();
                if (!evaluateOutboundRequirements(ownerIdHolder, fieldAcInfo)) {
                    try {
                        ReflectionUtils.setFieldValue(object, fieldName, null);
                    } catch (Exception ex) {
                        throw new AccessControlMisconfigurationException("Anonymization failed. Can't set a value for a field ." + fieldName, ex);
                    }
                    anonymizedFields.add(fieldName);
                }
            }
        }
        return anonymizedFields;
    }

}
