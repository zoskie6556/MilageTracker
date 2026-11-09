package com.fueltrack;

import com.fueltrack.service.AiRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AiRateLimiterTest {

    @Test
    void enforcesSlidingWindowIndependentlyForEachUser() {
        Instant start = Instant.parse("2026-11-09T10:00:00Z");
        AiRateLimiter limiter = new AiRateLimiter(
                Clock.fixed(start, ZoneOffset.UTC), 2, Duration.ofMinutes(1));

        assertThat(limiter.allowCall(1L)).isTrue();
        assertThat(limiter.allowCall(1L)).isTrue();
        assertThat(limiter.allowCall(1L)).isFalse();
        assertThat(limiter.allowCall(2L)).isTrue();
    }

    @Test
    void allowsCallsAgainWhenPreviousWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-09T10:00:00Z"));
        AiRateLimiter limiter = new AiRateLimiter(clock, 1, Duration.ofMinutes(1));

        assertThat(limiter.allowCall(1L)).isTrue();
        assertThat(limiter.allowCall(1L)).isFalse();
        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.allowCall(1L)).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
