package pro.api4.jsonapi4j.plugin.ac.report;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the {@code meta} member describing what access control hid, and merges it into whatever meta the
 * application already produced.
 *
 * <p>A response only carries this when something was actually hidden, so its presence is the answer to
 * "was anything withheld?" — an empty {@code data} with no report means there was nothing to return.
 */
@Slf4j
public final class AnonymizationReport {

    /** The {@code meta} member this writes under. */
    public static final String META_KEY = "accessControl";

    static final String ANONYMIZED = "anonymized";
    static final String FULLY_ANONYMIZED = "fullyAnonymized";
    static final String FIELDS = "fields";
    static final String PATH = "path";
    static final String REASON = "reason";

    private static boolean nonMapMetaReported;

    private AnonymizationReport() {

    }

    /**
     * Builds the report for one anonymization outcome.
     *
     * @return the value to place under {@link #META_KEY}, or {@code null} when there is nothing to say
     */
    public static Map<String, Object> of(AnonymizationResult<?> result, AnonymizationReportLevel level) {
        if (result == null || !level.isEnabled() || result.isNothingAnonymized()) {
            return null;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put(ANONYMIZED, true);
        if (result.isFullyAnonymized()) {
            report.put(FULLY_ANONYMIZED, true);
        }
        if (level.includesFields() && !result.isFullyAnonymized()) {
            report.put(FIELDS, fields(result.anonymizedFields(), level));
        }
        return report;
    }

    /**
     * The aggregate a document carries when several objects were anonymized under it: that something was,
     * without repeating what — each resource names its own hidden paths in its own meta.
     *
     * @return the value to place under {@link #META_KEY}, or {@code null} when nothing was hidden or the
     *         level is {@code NONE}
     */
    public static Map<String, Object> indicator(boolean anythingAnonymized, AnonymizationReportLevel level) {
        if (!anythingAnonymized || !level.isEnabled()) {
            return null;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put(ANONYMIZED, true);
        return report;
    }

    private static List<Map<String, Object>> fields(Map<String, ErrorCode> anonymized,
                                                    AnonymizationReportLevel level) {
        List<Map<String, Object>> fields = new ArrayList<>();
        anonymized.forEach((path, errorCode) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put(PATH, path);
            if (level.includesReasons() && errorCode != null) {
                entry.put(REASON, errorCode.toCode());
            }
            fields.add(entry);
        });
        return fields;
    }

    /**
     * Returns the meta to use in place of {@code currentMeta}, with the report added under
     * {@link #META_KEY}.
     *
     * <p>Meta is declared {@code Object} rather than {@code Map}, so an application may return a type of
     * its own. There is nowhere to add a member on such a value, so it is left alone and the report is
     * dropped — reported once, since it is a property of the application rather than of the request.
     *
     * @return the merged meta, or {@code currentMeta} unchanged when there is nothing to add or nowhere to
     *         add it
     */
    @SuppressWarnings("unchecked")
    public static Object mergeInto(Object currentMeta, Map<String, Object> report) {
        if (report == null) {
            return currentMeta;
        }
        if (currentMeta == null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put(META_KEY, report);
            return meta;
        }
        if (currentMeta instanceof Map<?, ?> map) {
            Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) map);
            merged.put(META_KEY, report);
            return merged;
        }
        if (!nonMapMetaReported) {
            nonMapMetaReported = true;
            log.warn("Access control cannot add its anonymization report to a '{}' meta — only a Map can "
                            + "carry an extra member. Return a Map from your meta resolvers, or set "
                            + "jsonapi4j.ac.anonymizationReport=NONE.",
                    currentMeta.getClass().getName());
        }
        return currentMeta;
    }

}
