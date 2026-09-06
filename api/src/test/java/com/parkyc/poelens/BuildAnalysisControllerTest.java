package com.parkyc.poelens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.MechanicDetail;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BuildAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NarrativeRefiner narrativeRefiner;

    @BeforeEach
    void useSuccessfulAiNarrative() {
        when(narrativeRefiner.refine(any(), anyList())).thenReturn(new NarrativeResult(
                "AI가 생성한 빌드 요약입니다.",
                List.of(new Mechanic("공격이 작동하는 과정: Fire Trap", "Fire Trap을 던져 적에게 피해를 줍니다.",
                        List.of(new MechanicDetail("덫 투척", "적이 밟을 위치에 덫을 던집니다.", "step")))),
                List.of(new Mechanic("피해 경감: armour", "방어도로 물리 피해를 줄입니다.")),
                List.of(new Mechanic("방어 버프: Determination", "Determination으로 방어도를 높입니다."))));
    }

    @Test
    void acceptsOptionalOperationFactsAndKeepsOmittedFactsNull() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BuildFacts supplied = objectMapper.readValue("""
                { "operationFacts": [{
                  "sourceType": "item", "sourceName": "Any Item", "action": "gain",
                  "subject": "frenzy-charge", "effects": ["ChanceToGainFrenzyChargeOnHit"]
                }] }
                """, BuildFacts.class);
        BuildFacts omitted = objectMapper.readValue("{}", BuildFacts.class);

        assertThat(supplied.operationFacts()).singleElement()
                .extracting(fact -> fact.sourceType(), fact -> fact.subject())
                .containsExactly("item", "frenzy-charge");
        assertThat(omitted.operationFacts()).isNull();
    }

    @Test
    void returnsAiNarrativeWithStructuredBuildDetails() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameVersion": "3.29",
                                  "buildFacts": {
                                    "offence": [{ "name": "Fire Trap", "role": "primary", "delivery": "trap", "tags": ["fire"] }],
                                    "defence": [{ "kind": "armour", "value": 21000 }],
                                    "buffs": [{ "name": "Determination", "kind": "aura", "appliesTo": "player", "tags": ["armour"] }],
                                    "passives": [{ "name": "Growth and Decay", "kind": "notable", "effects": ["Regenerate 1% of Life per second"], "tags": ["life-regeneration"] }],
                                    "items": [{ "slot": "Weapon 1", "name": "Doom Branch", "modifiers": ["+90 to maximum Life"] }],
                                    "performance": { "totalDps": 123456 }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.summary").value("AI가 생성한 빌드 요약입니다."))
                .andExpect(jsonPath("$.returnObject.offence[0].explanation").value("Fire Trap을 던져 적에게 피해를 줍니다."))
                .andExpect(jsonPath("$.returnObject.offence[0].details[0].label").value("덫 투척"))
                .andExpect(jsonPath("$.returnObject.offence[0].details[0].type").value("step"))
                .andExpect(jsonPath("$.returnObject.passiveNodes[0].title").value("주요 패시브: Growth and Decay"))
                .andExpect(jsonPath("$.returnObject.gear[0].title").value("장비: Weapon 1 · Doom Branch"));
    }

    @Test
    void returnsTooManyRequestsWhenTheDailyAiLimitIsReached() throws Exception {
        doThrow(new PoeLensException(ErrorCode.AI_DAILY_LIMIT_REACHED))
                .when(narrativeRefiner).refine(any(), anyList());

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"gameVersion\": \"3.29\", \"buildFacts\": {} }"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AI_DAILY_LIMIT_REACHED"))
                .andExpect(jsonPath("$.message").value("금일 AI 분석 리미트에 도달했습니다."));
    }
}
