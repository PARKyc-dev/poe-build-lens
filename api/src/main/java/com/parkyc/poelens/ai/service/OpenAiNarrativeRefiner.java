package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAiNarrativeRefiner implements NarrativeRefiner {
    private static final Logger log = LogManager.getLogger(OpenAiNarrativeRefiner.class);
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final PromptLogWriter promptLogWriter;
    private final OpenAiResponseParser responseParser;
    private final BuildNarrativePromptBuilder promptBuilder;
    private final OpenAiResponsesClient responsesClient;
    private final OpenAiNarrativeSchema narrativeSchema;
    private final NarrativeSectionValidator sectionValidator;
    private final OpenAiDailyUsageLimiter usageLimiter;

    @Value("${poe-lens.openai.enabled:false}")
    private boolean enabled;

    @Value("${poe-lens.openai.api-key:}")
    private String apiKey;

    @Value("${poe-lens.openai.model}")
    private String model;

    public OpenAiNarrativeRefiner(PromptLogWriter promptLogWriter, OpenAiResponseParser responseParser,
                                  BuildNarrativePromptBuilder promptBuilder, OpenAiResponsesClient responsesClient,
                                  OpenAiNarrativeSchema narrativeSchema, NarrativeSectionValidator sectionValidator,
                                  OpenAiDailyUsageLimiter usageLimiter) {
        this.promptLogWriter = promptLogWriter;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.responsesClient = responsesClient;
        this.narrativeSchema = narrativeSchema;
        this.sectionValidator = sectionValidator;
        this.usageLimiter = usageLimiter;
    }

    @Override
    public NarrativeResult refine(BuildFacts facts, List<OperationFlow> operationFlows) {
        if (!enabled || apiKey.isBlank()) {
            log.error("OpenAI 분석을 사용할 수 없음: 사용 설정={}, API 키 설정={}", enabled, !apiKey.isBlank());
            throw new PoeLensException(ErrorCode.AI_GENERATION_FAILED);
        }
        try {
            if (!usageLimiter.tryConsume()) {
                log.warn("OpenAI 일일 호출 한도에 도달했습니다");
                throw new PoeLensException(ErrorCode.AI_DAILY_LIMIT_REACHED);
            }
        } catch (PoeLensException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("OpenAI 사용량 확인에 실패했습니다", exception);
            throw new PoeLensException(ErrorCode.AI_GENERATION_FAILED);
        }

        var request = queue.submit(() -> generate(facts, operationFlows));
        try {
            return request.get(95, TimeUnit.SECONDS);
        } catch (Exception exception) {
            request.cancel(true);
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            Throwable cause = exception instanceof ExecutionException && exception.getCause() != null ? exception.getCause() : exception;
            log.error("OpenAI 분석 문장 생성에 실패했습니다", cause);
            throw new PoeLensException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private NarrativeResult generate(BuildFacts facts, List<OperationFlow> operationFlows) throws Exception {
        String prompt = promptBuilder.build(facts, operationFlows);
        log.info("OpenAI 기재 요청: 모델={}, 프롬프트 길이={}", model, prompt.length());
        String responseBody = responsesClient.request(apiKey, model, prompt, narrativeSchema.create(facts, operationFlows));
        Map<String, Object> narrative;
        try {
            narrative = responseParser.parse(responseBody);
            promptLogWriter.write(prompt, responseParser.formatForLog(narrative));
        } catch (Exception exception) {
            promptLogWriter.write(prompt, responseBody);
            throw exception;
        }
        return sectionValidator.validate(narrative, facts, operationFlows);
    }
}
