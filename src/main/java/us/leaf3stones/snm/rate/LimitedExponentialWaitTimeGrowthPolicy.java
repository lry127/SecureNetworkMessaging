package us.leaf3stones.snm.rate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimitedExponentialWaitTimeGrowthPolicy implements RateLimiting.RateLimitingPolicy {
    static final int REFRESH_INTERVAL_MILLIS = 30 * 1000; // 5 sec
    static final int LOG_RESET_INTERVAL_MILLIS = 60 * 1000; // 1 min
    static final int BASE_WAITING_MILLIS = 125; // 0.125 sec
    static final int MAX_WAITING_MILLIS = 10 * 1000; // 10 sec
    static final int EXCESSIVE_REQUESTS_THRESHOLD = 25;

    static final int DETAILED_EXAMINE_THRESHOLD = 10;

    @Override
    public int getRefreshIntervalMillis() {
        return REFRESH_INTERVAL_MILLIS;
    }

    @Override
    public void onRefresh(Map<Integer, RateLimiting.AccessLog> accessMap, long currTime) {
        List<Integer> expiredLog = new ArrayList<>();
        long expireMin = currTime - LOG_RESET_INTERVAL_MILLIS;
        for (Map.Entry<Integer, RateLimiting.AccessLog> entry : accessMap.entrySet()) {
            RateLimiting.AccessLog log = entry.getValue();
            if (log.accesses.isEmpty()) {
                continue;
            }
            if (log.accesses.size() < DETAILED_EXAMINE_THRESHOLD) {
                if (log.accesses.getLast() < expireMin) {
                    expiredLog.add(entry.getKey());
                }
            } else {
                log.accesses.removeIf(accessedTime -> accessedTime < expireMin);
            }
        }
        expiredLog.forEach(accessMap::remove);
    }

    @Override
    public int getWaitingTimeFor(int ip, RateLimiting.AccessLog accessLog) {
        int accessedTimes = accessLog.accesses.size();
        accessLog.accesses.add(System.currentTimeMillis());
        if (accessedTimes == 0) {
            return 0;
        } else if (accessedTimes > EXCESSIVE_REQUESTS_THRESHOLD) {
            throw new RateLimiting.TooManyRequestException("too many requests. attempted: " + accessedTimes);
        }
        int waitingFactor = (int) Math.pow(2, accessedTimes);
        int computedWaitingMillis = waitingFactor * BASE_WAITING_MILLIS;
        return Math.min(computedWaitingMillis, MAX_WAITING_MILLIS);
    }
}
