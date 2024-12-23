package us.leaf3stones.snm.demo.arithmetic;

import us.leaf3stones.snm.rate.RateLimiting;

import java.util.ArrayList;
import java.util.Map;

public class CalculatorRateLimiting implements RateLimiting.RateLimitingPolicy {
    private static final int REFRESH_INTERVAL_MILLIS = 10_000; // 10 sec
    private static final int LOG_RESET_INTERVAL_MILLIS = 30_000; // 30 sec

    @Override
    public int getRefreshIntervalMillis() {
        return REFRESH_INTERVAL_MILLIS;
    }

    @Override
    public void onRefresh(Map<Integer, RateLimiting.AccessLog> accessMap, long currTime) {
        long expireMin = currTime - LOG_RESET_INTERVAL_MILLIS;
        ArrayList<Integer> expired = new ArrayList<>();
        for (Map.Entry<Integer, RateLimiting.AccessLog> entry : accessMap.entrySet()) {
            RateLimiting.AccessLog log = entry.getValue();
            if (log.accesses.isEmpty()) {
                expired.add(entry.getKey());
                continue;
            }
            log.accesses.removeIf(accessedTime -> accessedTime < expireMin);
        }
        expired.forEach(accessMap::remove);
    }

    @Override
    public int getWaitingTimeFor(int ip, RateLimiting.AccessLog accessLog) {
        if (accessLog.accesses.size() < 3) {
            accessLog.accesses.add(System.currentTimeMillis());
            return 0;
        }
        throw new RateLimiting.TooManyRequestException("this ip requested more than 3 connections within 30 secs, " +
                "rejecting new connections");
    }
}
