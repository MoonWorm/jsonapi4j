package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeadlineTests {

    @Nested
    class Await {

        @Test
        void await_noDeadline_waitsForResult() throws Exception {
            Deadline sut = Deadline.after(0);

            assertThat(sut.await(CompletableFuture.supplyAsync(() -> "done"))).isEqualTo("done");
        }

        @Test
        void await_completesBeforeDeadline_returnsResult() throws Exception {
            Deadline sut = Deadline.after(1000);

            assertThat(sut.await(CompletableFuture.completedFuture("done"))).isEqualTo("done");
        }

        @Test
        void await_stillRunningAtDeadline_throwsTimeoutException() {
            Deadline sut = Deadline.after(20);

            assertThatThrownBy(() -> sut.await(new CompletableFuture<String>())).isInstanceOf(TimeoutException.class);
        }

        @Test
        void await_failedFuture_throwsExecutionException() {
            Deadline sut = Deadline.after(1000);

            assertThatThrownBy(() -> sut.await(CompletableFuture.failedFuture(new IllegalStateException("boom"))))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        }

    }

    @Nested
    class Cap {

        @Test
        void cap_noDeadline_keepsTimeout() {
            assertThat(Deadline.none().cap(Duration.ofSeconds(5))).isEqualTo(Duration.ofSeconds(5));
        }

        @Test
        void cap_lessTimeLeft_shortensTimeout() {
            Deadline sut = Deadline.after(1000);

            assertThat(sut.cap(Duration.ofSeconds(5))).isLessThanOrEqualTo(Duration.ofSeconds(1));
        }

    }

}
