package pro.api4.jsonapi4j.principal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedPrincipalContextHolderTests {

    private static final String USER_ID = "user-42";

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.clear();
    }

    private record TestPrincipal(List<String> authenticatedClientEntitlements,
                                 Set<String> authenticatedClientScopes,
                                 String authenticatedUserId,
                                 Map<String, Object> attributes) implements Principal {
    }

    private static Principal principal(String userId) {
        return new TestPrincipal(List.of("ADMIN"), Set.of("users.read"), userId, Map.of());
    }

    @Test
    void clear_principalWasSet_leavesNothingBehind() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principal(USER_ID));

        AuthenticatedPrincipalContextHolder.clear();

        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
        assertThat(AuthenticatedPrincipalContextHolder.getEntitlements()).isEmpty();
        assertThat(AuthenticatedPrincipalContextHolder.getScopes()).isEmpty();
    }

    @Test
    void setAuthenticatedPrincipalContext_null_clearsTheContext() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principal(USER_ID));

        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(null);

        assertThat(AuthenticatedPrincipalContextHolder.getPrincipal()).isEmpty();
    }

    @Test
    void clear_onAPooledThread_nextTaskSeesNoPrincipal() throws Exception {
        // a worker that sets a principal and clears it must not leak it to the next task
        // scheduled onto the same pooled thread
        ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
        try {
            singleThreadPool.submit(() -> {
                AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principal("first-caller"));
                try {
                    return AuthenticatedPrincipalContextHolder.getAuthenticatedUserId();
                } finally {
                    AuthenticatedPrincipalContextHolder.clear();
                }
            }).get();

            assertThat(observedUserIdOn(singleThreadPool)).isEmpty();
        } finally {
            singleThreadPool.shutdownNow();
            assertThat(singleThreadPool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void setWithoutClear_onAPooledThread_leaksToTheNextTask() throws Exception {
        // pins why clearing matters: without it the identity survives on the pooled thread
        ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
        try {
            singleThreadPool.submit(() ->
                    AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principal("first-caller"))
            ).get();

            assertThat(observedUserIdOn(singleThreadPool)).contains("first-caller");
        } finally {
            singleThreadPool.shutdownNow();
            assertThat(singleThreadPool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static java.util.Optional<String> observedUserIdOn(ExecutorService pool)
            throws ExecutionException, InterruptedException {
        return pool.submit(AuthenticatedPrincipalContextHolder::getAuthenticatedUserId).get();
    }

}
