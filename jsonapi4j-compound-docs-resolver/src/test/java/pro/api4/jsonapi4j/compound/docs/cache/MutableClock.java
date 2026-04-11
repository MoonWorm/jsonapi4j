package pro.api4.jsonapi4j.compound.docs.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test utility — a Clock whose instant can be advanced programmatically.
 */
class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    MutableClock(Instant initial) {
        this.instant = new AtomicReference<>(initial);
        this.zone = ZoneOffset.UTC;
    }

    void advance(Duration duration) {
        instant.updateAndGet(i -> i.plus(duration));
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

}
