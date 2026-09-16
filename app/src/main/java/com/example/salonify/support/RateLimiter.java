package com.example.salonify.support;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プロセス内での固定ウィンドウ方式の rate limiter(リファレンス実装の lib/rate-limit.ts に対応)。
 * 分散環境には対応していない — MVPのみを想定。
 */
@Component
public class RateLimiter {

    private static final class Bucket {
        int count;
        long resetAtMs;
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 許可される場合は true、現在のウィンドウ内で制限を超えている場合は false を返す。 */
    public synchronized boolean allow(String key, int limit, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Bucket b = buckets.get(key);
        if (b == null || now >= b.resetAtMs) {
            Bucket nb = new Bucket();
            nb.count = 1;
            nb.resetAtMs = now + windowMs;
            buckets.put(key, nb);
            return true;
        }
        if (b.count >= limit) {
            return false;
        }
        b.count++;
        return true;
    }
}
