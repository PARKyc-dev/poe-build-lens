package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponseParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public Map<String, String> parse(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
        if (output == null) {
            return Map.of();
        }

        for (Map<String, Object> item : output) {
            if (!"message".equals(item.get("type"))) {
                continue;
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) item.get("content");
            if (content == null) {
                continue;
            }

            for (Map<String, Object> part : content) {
                if ("output_text".equals(part.get("type")) && part.get("text") instanceof String text) {
                    return objectMapper.readValue(text, new TypeReference<>() {});
                }
            }
        }

        return Map.of();
    }
}
