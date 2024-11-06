package us.leaf3stones.snm.rate;

import java.util.Map;

public class UnlimitedRateLimitingPolicy implements RateLimiting.RateLimitingPolicy {
    @Override
    public int getRefreshIntervalMillis() {
        return -1;
    }

    @Override
    public void onRefresh(Map<Integer, RateLimiting.AccessLog> accessMap, long currTime) {

    }

    @Override
    public int getWaitingTimeFor(int ip, RateLimiting.AccessLog accessLog) {
        return 0;
    }
}
