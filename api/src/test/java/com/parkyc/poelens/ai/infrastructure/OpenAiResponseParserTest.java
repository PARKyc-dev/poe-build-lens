package com.parkyc.poelens.ai.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

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

    @Test
    void readsStructuredOffenceSections() throws Exception {
        String responseBody = """
                {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\"offenceSections\\":[{\\"attackName\\":\\"Fire Trap\\",\\"section\\":\\"core\\",\\"explanation\\":\\"덫을 설치해 피해를 줍니다.\\",\\"evidence\\":[\\"Fire Trap\\"]}]}"}]}]}
                """;

        Map<String, Object> parsed = new OpenAiResponseParser().parse(responseBody);
        assertThat(parsed.get("offenceSections")).isInstanceOf(List.class);
    }
}
