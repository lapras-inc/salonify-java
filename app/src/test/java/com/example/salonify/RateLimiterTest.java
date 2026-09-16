package com.example.salonify;

import com.example.salonify.support.RateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 3; i++) {
            assertTrue(rl.allow("k", 3, 60_000), "attempt " + i + " should be allowed");
        }
        assertFalse(rl.allow("k", 3, 60_000), "4th attempt should be blocked");
    }

    @Test
    void separateKeysAreIndependent() {
        RateLimiter rl = new RateLimiter();
        assertTrue(rl.allow("a", 1, 60_000));
        assertFalse(rl.allow("a", 1, 60_000));
        assertTrue(rl.allow("b", 1, 60_000));
    }

    @Test
    void windowResetsAllowsAgain() throws InterruptedException {
        RateLimiter rl = new RateLimiter();
        assertTrue(rl.allow("w", 1, 50));
        assertFalse(rl.allow("w", 1, 50));
        java.lang.Thread.sleep(60);
        assertTrue(rl.allow("w", 1, 50));
    }
}
