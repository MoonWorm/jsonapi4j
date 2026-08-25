package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;

/**
 * The outcome of evaluating access control requirements.
 *
 * <p>When access is refused, carries the {@link ErrorCode} to report to the caller. Any {@code ErrorCode}
 * may be used, including one defined by the application.
 *
 * @param granted   whether the requirements were satisfied
 * @param errorCode the code to report, {@code null} when granted
 */
public record EvaluationResult(boolean granted, ErrorCode errorCode) {

    private static final EvaluationResult ALLOWED = new EvaluationResult(true, null);

    /**
     * Access is allowed.
     */
    public static EvaluationResult allowed() {
        return ALLOWED;
    }

    /**
     * Access is refused.
     *
     * @param errorCode the code to report to the caller
     */
    public static EvaluationResult denied(ErrorCode errorCode) {
        return new EvaluationResult(false, errorCode);
    }

    public boolean isDenied() {
        return !granted;
    }

}
