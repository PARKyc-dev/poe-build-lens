package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.ai.domain.dto.OpenAiUsage;
import com.parkyc.poelens.ai.repository.OpenAiDailyUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class OpenAiDailyUsageLimiter {
    private final OpenAiDailyUsageRepository repository;
    private final int dailyLimit;
    private final ZoneId zoneId;

    public OpenAiDailyUsageLimiter(OpenAiDailyUsageRepository repository,
                                   @Value("${poe-lens.openai.daily-limit:100}") int dailyLimit,
                                   @Value("${poe-lens.openai.daily-limit-zone:Asia/Seoul}") String zoneId) {
        this.repository = repository;
        this.dailyLimit = dailyLimit;
        this.zoneId = ZoneId.of(zoneId);
    }

    public boolean tryConsume() {
        return repository.tryIncrement(LocalDate.now(zoneId), dailyLimit);
    }

    public OpenAiUsage currentUsage() {
        return new OpenAiUsage(repository.currentCount(LocalDate.now(zoneId)), dailyLimit);
    }
}
