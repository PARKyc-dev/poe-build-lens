package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponseParser {
    private static final Logger log = LogManager.getLogger(OpenAiResponseParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public Map<String, String> parse(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
        if (output == null) {
            log.warn("OpenAI 응답에 output 필드가 없습니다");
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

        log.warn("OpenAI 응답에 message output_text가 없습니다");
        return Map.of();
    }

    public String formatForLog(Map<String, String> response) throws Exception {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
