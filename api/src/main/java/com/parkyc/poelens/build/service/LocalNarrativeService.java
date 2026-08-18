package com.parkyc.poelens.build.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class LocalNarrativeService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final HttpClient client = HttpClient.newHttpClient();
    @Value("${poe-lens.openai.enabled:false}") private boolean enabled;
    @Value("${poe-lens.openai.api-key:}") private String apiKey;
    @Value("${poe-lens.openai.model:gpt-5.4-nano}") private String model;

    public List<List<Mechanic>> refine(BuildFacts facts, List<Mechanic> offence, List<Mechanic> defence) {
        if (!enabled || apiKey.isBlank()) return List.of(offence, defence);
        try {
            return queue.submit(() -> generate(facts, offence, defence)).get(45, TimeUnit.SECONDS);
        } catch (Exception ignored) { return List.of(offence, defence); }
    }

    private List<List<Mechanic>> generate(BuildFacts facts, List<Mechanic> offence, List<Mechanic> defence) throws Exception {
        String prompt = "주어진 PoB 사실 밖의 정보는 쓰지 말고 한국어로 공격과 방어를 각각 한 문장으로 설명해. 사실=" + objectMapper.writeValueAsString(facts);
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("offenceSummary", Map.of("type", "string"), "defenceSummary", Map.of("type", "string")), "required", List.of("offenceSummary", "defenceSummary"), "additionalProperties", false);
        String body = objectMapper.writeValueAsString(Map.of("model", model, "input", prompt, "text", Map.of("format", Map.of("type", "json_schema", "name", "build_narrative", "strict", true, "schema", schema))));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses")).timeout(Duration.ofSeconds(40)).header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        Map<String, Object> response = objectMapper.readValue(client.send(request, HttpResponse.BodyHandlers.ofString()).body(), new TypeReference<>() {});
        List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
        List<Map<String, Object>> content = (List<Map<String, Object>>) output.getFirst().get("content");
        Map<String, String> narrative = objectMapper.readValue((String) content.getFirst().get("text"), new TypeReference<>() {});
        String offenceText = narrative.get("offenceSummary");
        String defenceText = narrative.get("defenceSummary");
        if (offenceText == null || defenceText == null) return List.of(offence, defence);
        return List.of(List.of(new Mechanic("공격 기재", offenceText)), List.of(new Mechanic("방어 기재", defenceText)));
    }
}
