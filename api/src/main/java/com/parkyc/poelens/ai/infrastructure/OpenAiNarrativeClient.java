package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
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
    public List<List<Mechanic>> refine(BuildFacts facts, List<Mechanic> offence, List<Mechanic> defence) {
        if (!enabled || apiKey.isBlank()) {
            return List.of(offence, defence);
        }

        try {
            return queue.submit(() -> generate(facts, offence, defence)).get(45, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            return List.of(offence, defence);
        }
    }

    private List<List<Mechanic>> generate(BuildFacts facts,
                                          List<Mechanic> offence, List<Mechanic> defence) throws Exception {
        String prompt = promptBuilder.build(facts);

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "offenceSummary", Map.of("type", "string"),
                        "defenceSummary", Map.of("type", "string")),
                "required", List.of("offenceSummary", "defenceSummary"),
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

        String responseBody = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        promptLogWriter.write(prompt, responseBody);
        Map<String, String> narrative = responseParser.parse(responseBody);

        String offenceText = narrative.get("offenceSummary");
        String defenceText = narrative.get("defenceSummary");

        if (offenceText == null || defenceText == null) {
            return List.of(offence, defence);
        }

        return List.of(
                List.of(new Mechanic("공격 기재", offenceText)),
                List.of(new Mechanic("방어 기재", defenceText)));
    }
}
