package com.parkyc.poelens.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class OpenAiResponsesClient {
    private static final Logger log = LogManager.getLogger(OpenAiResponsesClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public String request(String apiKey, String model, String prompt, Map<String, Object> schema) throws Exception {
        long startedAt = System.nanoTime();
        String body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "build_narrative",
                        "strict", true,
                        "schema", schema))));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("OpenAI 기재 응답 수신: 모델={}, 상태={}, 처리 시간(ms)={}", model, response.statusCode(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        return response.body();
    }
}
