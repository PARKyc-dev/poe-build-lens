package com.parkyc.poelens.ai.repository;

import java.time.LocalDate;

public interface OpenAiDailyUsageRepository {
    boolean tryIncrement(LocalDate date, int limit);

    int currentCount(LocalDate date);
}
