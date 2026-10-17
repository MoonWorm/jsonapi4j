package pro.api4.jsonapi4j.compound.docs;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The point in time by which the whole include resolution of a request must finish - see {@code includesTimeoutMs}.
 * Every fetch made for the request shares it, so no single fetch may wait longer than the time left.
 */
public final class Deadline {

    private static final Deadline NONE = new Deadline(Long.MAX_VALUE);

    private final long expiresAtNanos;

    private Deadline(long expiresAtNanos) {
        this.expiresAtNanos = expiresAtNanos;
    }

    /**
     * @param timeoutMs the time budget, starting now; {@code 0} or less means no deadline
     */
    public static Deadline after(long timeoutMs) {
        if (timeoutMs <= 0) {
            return NONE;
        }
        return new Deadline(System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos());
    }

    /**
     * @return a deadline that never expires
     */
    public static Deadline none() {
        return NONE;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isExpired() {
        return !isNone() && System.nanoTime() - expiresAtNanos >= 0;
    }

    /**
     * Waits for {@code future} at most until this deadline - for as long as it takes when there is none.
     *
     * @throws TimeoutException when the deadline passes first, or has passed already
     */
    public <RESULT> RESULT await(Future<RESULT> future)
            throws ExecutionException, InterruptedException, TimeoutException {
        if (isNone()) {
            return future.get();
        }
        return future.get(remaining().toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * @return the time left - {@link Duration#ZERO} once expired - or {@code null} when there is no deadline
     */
    private Duration remaining() {
        if (isNone()) {
            return null;
        }
        long remainingNanos = expiresAtNanos - System.nanoTime();
        return remainingNanos > 0 ? Duration.ofNanos(remainingNanos) : Duration.ZERO;
    }

    /**
     * @return {@code timeout}, shortened to the time left when that is less
     */
    public Duration cap(Duration timeout) {
        Duration remaining = remaining();
        return remaining == null || remaining.compareTo(timeout) >= 0 ? timeout : remaining;
    }

}
