package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.ai.repository.OpenAiDailyUsageRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiDailyUsageLimiterTest {

    @Test
    void allowsOneHundredRequestsAndRejectsTheNextRequest() {
        InMemoryUsageRepository repository = new InMemoryUsageRepository();
        OpenAiDailyUsageLimiter limiter = new OpenAiDailyUsageLimiter(repository, 100, "Asia/Seoul");

        for (int request = 0; request < 100; request++) {
            assertThat(limiter.tryConsume()).isTrue();
        }

        assertThat(limiter.tryConsume()).isFalse();
    }

    @Test
    void usesTheConfiguredDailyLimit() {
        InMemoryUsageRepository repository = new InMemoryUsageRepository();
        OpenAiDailyUsageLimiter limiter = new OpenAiDailyUsageLimiter(repository, 2, "Asia/Seoul");

        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isFalse();
    }

    @Test
    void returnsTheStoredUsageAndConfiguredLimit() {
        InMemoryUsageRepository repository = new InMemoryUsageRepository();
        OpenAiDailyUsageLimiter limiter = new OpenAiDailyUsageLimiter(repository, 37, "Asia/Seoul");
        limiter.tryConsume();

        assertThat(limiter.currentUsage().used()).isEqualTo(1);
        assertThat(limiter.currentUsage().limit()).isEqualTo(37);
    }

    private static class InMemoryUsageRepository implements OpenAiDailyUsageRepository {
        private final Map<LocalDate, Integer> usage = new HashMap<>();

        @Override
        public boolean tryIncrement(LocalDate date, int limit) {
            int current = usage.getOrDefault(date, 0);
            if (current >= limit) return false;
            usage.put(date, current + 1);
            return true;
        }

        @Override
        public int currentCount(LocalDate date) {
            return usage.getOrDefault(date, 0);
        }
    }
}
