package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Value("${poe-lens.openai.enabled:false}")
    private boolean enabled;

    @Value("${poe-lens.openai.api-key:}")
    private String apiKey;

    @Value("${poe-lens.openai.model}")
    private String model;

    public OpenAiNarrativeRefiner(PromptLogWriter promptLogWriter, OpenAiResponseParser responseParser,
                                  BuildNarrativePromptBuilder promptBuilder, OpenAiResponsesClient responsesClient,
                                  OpenAiNarrativeSchema narrativeSchema, NarrativeSectionValidator sectionValidator) {
        this.promptLogWriter = promptLogWriter;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.responsesClient = responsesClient;
        this.narrativeSchema = narrativeSchema;
        this.sectionValidator = sectionValidator;
    }

    @Override
    public NarrativeResult refine(BuildFacts facts, String summary, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs) {
        return refine(facts, List.of(), summary, offence, defence, buffs);
    }

    @Override
    public NarrativeResult refine(BuildFacts facts, List<OperationFlow> operationFlows, String summary, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs) {
        NarrativeResult fallback = new NarrativeResult(summary, offence, defence, buffs);
        if (!enabled || apiKey.isBlank()) {
            log.info("OpenAI 기재 보정 건너뜀: 사용 설정={}, API 키 설정={}", enabled, !apiKey.isBlank());
            return fallback;
        }

        var request = queue.submit(() -> generate(facts, operationFlows, fallback));
        try {
            return request.get(95, TimeUnit.SECONDS);
        } catch (Exception exception) {
            request.cancel(true);
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("OpenAI 기재 보정에 실패해 규칙 기반 기재를 사용합니다", exception);
            return fallback;
        }
    }

    private NarrativeResult generate(BuildFacts facts, List<OperationFlow> operationFlows, NarrativeResult fallback) throws Exception {
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
        NarrativeResult result = sectionValidator.validate(narrative, facts, operationFlows, fallback);
        if (result.offence() == fallback.offence()) {
            log.warn("공격 메커니즘 응답의 출처 또는 섹션 검증에 실패해 기본 안내를 사용합니다");
        }
        return result;
    }
}
