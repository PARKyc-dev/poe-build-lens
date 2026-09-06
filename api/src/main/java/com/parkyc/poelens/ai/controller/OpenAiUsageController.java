package com.parkyc.poelens.ai.controller;

import com.parkyc.poelens.ai.domain.dto.OpenAiUsage;
import com.parkyc.poelens.ai.service.OpenAiDailyUsageLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-usage")
public class OpenAiUsageController {
    private final OpenAiDailyUsageLimiter usageLimiter;

    public OpenAiUsageController(OpenAiDailyUsageLimiter usageLimiter) {
        this.usageLimiter = usageLimiter;
    }

    @GetMapping
    public OpenAiUsage currentUsage() {
        return usageLimiter.currentUsage();
    }
}
