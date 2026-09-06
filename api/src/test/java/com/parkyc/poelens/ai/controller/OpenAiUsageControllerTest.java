package com.parkyc.poelens.ai.controller;

import com.parkyc.poelens.ai.domain.dto.OpenAiUsage;
import com.parkyc.poelens.ai.service.OpenAiDailyUsageLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenAiUsageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenAiDailyUsageLimiter usageLimiter;

    @Test
    void returnsCurrentUsageAndConfiguredLimit() throws Exception {
        when(usageLimiter.currentUsage()).thenReturn(new OpenAiUsage(1, 37));

        mockMvc.perform(get("/api/ai-usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.used").value(1))
                .andExpect(jsonPath("$.returnObject.limit").value(37));
    }
}
