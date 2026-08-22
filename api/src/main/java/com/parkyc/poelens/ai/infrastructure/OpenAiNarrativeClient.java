package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAiNarrativeClient implements NarrativeRefiner {
    private static final Logger log = LogManager.getLogger(OpenAiNarrativeClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final HttpClient client = HttpClient.newHttpClient();
    private final PromptLogWriter promptLogWriter;
    private final OpenAiResponseParser responseParser;
    private final BuildNarrativePromptBuilder promptBuilder;

    @Value("${poe-lens.openai.enabled:false}")
    private boolean enabled;

    @Value("${poe-lens.openai.api-key:}")
    private String apiKey;

    @Value("${poe-lens.openai.model}")
    private String model;

    public OpenAiNarrativeClient(PromptLogWriter promptLogWriter, OpenAiResponseParser responseParser,
                                 BuildNarrativePromptBuilder promptBuilder) {
        this.promptLogWriter = promptLogWriter;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public List<List<Mechanic>> refine(BuildFacts facts, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs, List<Mechanic> mobility) {
        if (!enabled || apiKey.isBlank()) {
            log.info("OpenAI 기재 보정 건너뜀: 사용 설정={}, API 키 설정={}", enabled, !apiKey.isBlank());
            return List.of(offence, defence, buffs, mobility);
        }

        try {
            return queue.submit(() -> generate(facts, offence, defence, buffs, mobility)).get(45, TimeUnit.SECONDS);
        } catch (Exception exception) {
            log.warn("OpenAI 기재 보정에 실패해 규칙 기반 기재를 사용합니다", exception);
            return List.of(offence, defence, buffs, mobility);
        }
    }

    private List<List<Mechanic>> generate(BuildFacts facts,
                                          List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs, List<Mechanic> mobility) throws Exception {
        String prompt = promptBuilder.build(facts);
        long startedAt = System.nanoTime();
        log.info("OpenAI 기재 요청: 모델={}, 프롬프트 길이={}", model, prompt.length());

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "offenceSummary", Map.of("type", "string"),
                        "defenceSummary", Map.of("type", "string"), "buffSummary", Map.of("type", "string"), "mobilitySummary", Map.of("type", "string")),
                "required", List.of("offenceSummary", "defenceSummary", "buffSummary", "mobilitySummary"),
                "additionalProperties", false);
        String body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "build_narrative",
                        "strict", true,
                        "schema", schema))));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        log.info("OpenAI 기재 응답 수신: 모델={}, 상태={}, 처리 시간(ms)={}", model, response.statusCode(), elapsedMillis(startedAt));
        Map<String, String> narrative;
        try {
            narrative = responseParser.parse(responseBody);
            promptLogWriter.write(prompt, responseParser.formatForLog(narrative));
        } catch (Exception exception) {
            promptLogWriter.write(prompt, responseBody);
            throw exception;
        }

        String offenceText = narrative.get("offenceSummary");
        String defenceText = narrative.get("defenceSummary");
        String buffText = narrative.get("buffSummary");
        String mobilityText = narrative.get("mobilitySummary");

        if (offenceText == null || defenceText == null || buffText == null || mobilityText == null) {
            log.warn("OpenAI 기재 응답에 필요한 요약이 없습니다");
            return List.of(offence, defence, buffs, mobility);
        }

        return List.of(
                List.of(new Mechanic("공격 기재", offenceText)),
                List.of(new Mechanic("방어 기재", defenceText)),
                List.of(new Mechanic("버프 기재", buffText)),
                List.of(new Mechanic("이동기 기재", mobilityText)));
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
