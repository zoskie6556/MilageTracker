package com.fueltrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiRateLimiter {

    private final Clock clock;
    private final int maximumCalls;
    private final Duration window;
    private final Map<Long, ArrayDeque<Instant>> callsByUser = new HashMap<>();

    public AiRateLimiter(
            Clock clock,
            @Value("${app.ai.rate-limit.calls:5}") int maximumCalls,
            @Value("${app.ai.rate-limit.window:PT1M}") Duration window) {
        if (maximumCalls < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("AI rate limit must have a positive call count and time window.");
        }
        this.clock = clock;
        this.maximumCalls = maximumCalls;
        this.window = window;
    }

    /**
     * Reserves one AI request for an account if it remains within the configured sliding window.
     *
     * @param userId authenticated account identifier
     * @return true when the call is allowed
     */
    public synchronized boolean allowCall(Long userId) {
        Instant now = clock.instant();
        ArrayDeque<Instant> calls = callsByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        Instant cutoff = now.minus(window);
        while (!calls.isEmpty() && !calls.getFirst().isAfter(cutoff)) {
            calls.removeFirst();
        }
        if (calls.size() >= maximumCalls) {
            return false;
        }
        calls.addLast(now);
        return true;
    }

    /**
     * Returns the configured maximum calls allowed per time window.
     *
     * @return maximum request count
     */
    public int getMaximumCalls() {
        return maximumCalls;
    }

    /**
     * Returns the duration of the configured sliding window.
     *
     * @return window duration
     */
    public Duration getWindow() {
        return window;
    }
}
