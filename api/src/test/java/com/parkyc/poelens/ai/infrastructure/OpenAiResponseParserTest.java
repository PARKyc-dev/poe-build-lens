package com.parkyc.poelens.ai.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiResponseParserTest {

    @Test
    void readsOutputTextAfterReasoningOutput() throws Exception {
        String responseBody = """
                {
                  "output": [
                    {"type": "reasoning", "content": []},
                    {"type": "message", "content": [
                      {"type": "output_text", "text": "{\\"offenceSummary\\":\\"공격 문장\\",\\"defenceSummary\\":\\"방어 문장\\"}"}
                    ]}
                  ]
                }
                """;

        assertThat(new OpenAiResponseParser().parse(responseBody))
                .isEqualTo(Map.of("offenceSummary", "공격 문장", "defenceSummary", "방어 문장"));
    }

    @Test
    void formatsParsedResponseForReadableLogging() throws Exception {
        String formatted = new OpenAiResponseParser().formatForLog(Map.of(
                "offenceSummary", "공격 문장",
                "defenceSummary", "방어 문장"));

        assertThat(formatted).contains("\"offenceSummary\" : \"공격 문장\"");
        assertThat(formatted).doesNotContain("\\\"offenceSummary\\\"");
    }
}
