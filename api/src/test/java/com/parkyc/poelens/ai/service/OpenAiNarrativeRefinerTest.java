package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiNarrativeRefinerTest {

    @Test
    void rejectsAnalysisWhenTheDailyLimitIsReached() {
        OpenAiDailyUsageLimiter limiter = mock(OpenAiDailyUsageLimiter.class);
        when(limiter.tryConsume()).thenReturn(false);
        OpenAiNarrativeRefiner refiner = new OpenAiNarrativeRefiner(
                mock(PromptLogWriter.class), mock(OpenAiResponseParser.class), mock(BuildNarrativePromptBuilder.class),
                mock(OpenAiResponsesClient.class), mock(OpenAiNarrativeSchema.class),
                mock(NarrativeSectionValidator.class), limiter);
        ReflectionTestUtils.setField(refiner, "enabled", true);
        ReflectionTestUtils.setField(refiner, "apiKey", "test-key");

        assertThatThrownBy(() -> refiner.refine(
                new BuildFacts(null, null, null, null, null, null, null, null, null, null, null), List.of()))
                .isInstanceOf(PoeLensException.class)
                .extracting(exception -> ((PoeLensException) exception).errorCode())
                .isEqualTo(ErrorCode.AI_DAILY_LIMIT_REACHED);
    }
}
