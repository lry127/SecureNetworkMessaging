package us.leaf3stones.snm.rate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiting {
    private static RateLimiting instance;
    private final RateLimitingPolicy policy;
    private final ReentrantLock globalMapAccessLock;
    private final HashMap<Integer, AccessLog> accessMap;

    public RateLimiting(RateLimitingPolicy policy, ExecutorService executor) {
        this.policy = policy;
        globalMapAccessLock = new ReentrantLock();
        accessMap = new HashMap<>();
        feedPolicyWithDataRegularly(executor);
    }

    public RateLimiting(RateLimitingPolicy policy) {
        this(policy, Executors.newSingleThreadExecutor());
    }

    public static void init(ExecutorService executor, RateLimitingPolicy policy) {
        if (policy == null) {
            policy = new LimitedExponentialWaitTimeGrowthPolicy();
        }
        if (instance == null) {
            instance = new RateLimiting(policy, executor);
        }
    }

    public static void init(ExecutorService executor) {
        init(executor, null);
    }

    public static RateLimiting getInstance() {
        return instance;
    }

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    private void feedPolicyWithDataRegularly(ExecutorService executor) {
        int updateIntervalMillis = policy.getRefreshIntervalMillis();
        if (updateIntervalMillis < 0) {
            return;
        } else if (updateIntervalMillis < 10) {
            updateIntervalMillis = 10;
        }
        int finalUpdateIntervalMillis = updateIntervalMillis;
        executor.execute(() -> {
            while (true) {
                globalMapAccessLock.lock();
                policy.onRefresh(accessMap, System.currentTimeMillis());
                globalMapAccessLock.unlock();
                try {
                    Thread.sleep(finalUpdateIntervalMillis);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public int getWaitingTimeFor(int ip) {
        globalMapAccessLock.lock();
        AccessLog log = accessMap.computeIfAbsent(ip, k -> new AccessLog());
        int waitTime = policy.getWaitingTimeFor(ip, log);
        globalMapAccessLock.unlock();
        return waitTime;
    }

    public interface RateLimitingPolicy {
        int getRefreshIntervalMillis();

        void onRefresh(Map<Integer, AccessLog> accessMap, long currTime);

        int getWaitingTimeFor(int ip, AccessLog accessLog);
    }

    public static class AccessLog {
        public List<Long> accesses = new ArrayList<>();
    }

    public static class TooManyRequestException extends RuntimeException {
        public TooManyRequestException(String message) {
            super(message);
        }

        public TooManyRequestException(Exception e) {
            super(e);
        }
    }

}
