package us.leaf3stones.snm.rate;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


// todo: more through test
class LimitedExponentialWaitTimeGrowthPolicyTest {
    @Test
    void dontWaitWhenIpIsFirstSeen() {
        RateLimiting.RateLimitingPolicy policy = new LimitedExponentialWaitTimeGrowthPolicy();
        for (int i = 0; i < 100; ++i) {
            assertEquals(0, policy.getWaitingTimeFor(i, new RateLimiting.AccessLog()));
        }
    }

    @Test
    void exponentialGrowthWaitingWithinThreshold() {
        RateLimiting.RateLimitingPolicy policy = new LimitedExponentialWaitTimeGrowthPolicy();
        RateLimiting.AccessLog log = new RateLimiting.AccessLog();
        policy.getWaitingTimeFor(0, log);
        for (int i = 0; i < 3; ++i) {
            int actualWaiting = policy.getWaitingTimeFor(0, log);
            assertEquals(((int) (Math.pow(2, i + 1))) * LimitedExponentialWaitTimeGrowthPolicy.BASE_WAITING_MILLIS, actualWaiting);
        }
    }

    @Test
    void ensureMaxWaitingTimeIsRespected() {
        RateLimiting.RateLimitingPolicy policy = new LimitedExponentialWaitTimeGrowthPolicy();
        RateLimiting.AccessLog log = new RateLimiting.AccessLog();
        for (int i = 0; i < LimitedExponentialWaitTimeGrowthPolicy.EXCESSIVE_REQUESTS_THRESHOLD; ++i) {
            int actualWaiting = policy.getWaitingTimeFor(0, log);
            assertTrue(actualWaiting <= LimitedExponentialWaitTimeGrowthPolicy.MAX_WAITING_MILLIS);
        }
    }

    @Test
    void ensureWaitingIsReset() {
        RateLimiting.RateLimitingPolicy policy = new LimitedExponentialWaitTimeGrowthPolicy();
        HashMap<Integer, RateLimiting.AccessLog> map = new HashMap<>();
        for (int i = 0; i < 100; ++i) {
            RateLimiting.AccessLog log = new RateLimiting.AccessLog();
            map.put(i, log);
            policy.getWaitingTimeFor(i, log);
            policy.getWaitingTimeFor(i, log);
            policy.getWaitingTimeFor(i, log);
        }
        long fastForward = System.currentTimeMillis() + LimitedExponentialWaitTimeGrowthPolicy.LOG_RESET_INTERVAL_MILLIS + 1;
        policy.onRefresh(map, fastForward);
        assertEquals(0, map.size());
        for (int i = 0; i < 100; ++i) {
            assertEquals(0, policy.getWaitingTimeFor(i, new RateLimiting.AccessLog()));
        }
    }


}