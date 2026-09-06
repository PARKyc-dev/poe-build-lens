package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.AscendancyFact;
import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.ItemFact;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.OperationFact;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import com.parkyc.poelens.build.domain.dto.PerformanceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildNarrativePromptBuilderTest {

    @Test
    void includesOperationFlowsAndTheirGrounds() throws Exception {
        OperationFact consumesFrenzy = new OperationFact("skill", "Generic Attack", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFact gainsFrenzy = new OperationFact("item", "Generic Item", "gain", "frenzy-charge", List.of("Gain a Frenzy Charge on Hit"));
        BuildFacts facts = new BuildFacts(
                List.of(new OffenceFact("Generic Attack", "primary", 1.0, "attack", List.of(), List.of())),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(consumesFrenzy, gainsFrenzy), new PerformanceFact(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        OperationFlow flow = new OperationFlow("frenzy-charge", List.of("frenzy-charge 소비", "frenzy-charge 획득", "연속 사용"), List.of(consumesFrenzy, gainsFrenzy));

        String prompt = new BuildNarrativePromptBuilder().build(facts, List.of(flow));

        assertThat(prompt).contains("operationFlows", "frenzy-charge", "Generic Attack", "Generic Item", "frenzy-charge 소비");
    }

    @Test
    void includesMechanicsAndCrossSkillEquipmentEvidence() throws Exception {
        BuildFacts facts = new BuildFacts(
                List.of(new OffenceFact("Fire Trap", "primary", 730_000.0, "trap", List.of("fire", "damage-over-time"),
                        List.of(new AppliedModifierFact("Fire Damage", "INC", "Passive", false)))),
                List.of(new SkillFact("Fire Trap", 21, 20, "Default", true, false,
                        List.of(new SupportGemFact("Burning Damage", 20, 0, "Default", true, false,
                                List.of("Supports any skill that deals damage."))))),
                List.of(new DefenceFact("armour", 21_000.0), new DefenceFact("block", 70.0)),
                List.of(new BuffFact("Determination", "aura", "player", List.of("armour"))),
                List.of(),
                List.of(new PassiveFact("Arsonist", "notable", List.of("Regenerate 1.2% of Life per second"), List.of("life-regeneration"))),
                List.of(new AscendancyFact("Chieftain", "Hinekora, Death's Fury", List.of("Enemies you kill have a chance to Explode"), List.of("fire"))),
                List.of("armour", "life-regeneration"),
                List.of(new ItemFact("Weapon 1", "Secret Weapon", "Void Sceptre", "RARE", List.of("62% increased Fire Damage"), List.of("fire"))),
                List.of(),
                new PerformanceFact(0.0, 730_000.0, 5_800.0, 0.0, 800.0, 21_000.0, 0.0, 100_000.0));

        String prompt = new BuildNarrativePromptBuilder().build(facts);

        assertThat(prompt).contains("지속 피해", "덫", "방어도", "막기", "Fire Trap", "Supports any skill that deals damage.", "Determination", "Hinekora, Death's Fury");
        assertThat(prompt).contains("buildSummary", "offenceSections", "core", "supports", "modifiers", "operation", "attackName", "defenceSections", "defenceKind", "buffSections", "buffName", "section", "evidence", "details", "type=step", "type=interaction", "type=condition");
        assertThat(prompt).contains("주력 공격의 동작·운용 방식");
        assertThat(prompt).contains("buildSummary는 3~5문장", "숫자나 백분율을 쓰지 마", "상세 섹션으로 내려보내");
        assertThat(prompt).contains("첫 문장에 결론", "서로 다른 주제는 줄바꿈으로 구분", "방어 수치·방어층·회복 조건은 defenceSections");
        assertThat(prompt).contains("저항을 각각 별도 defenceSection으로 만들지 마", "defenceKind=resistances", "defenceKind=resistance-interaction");
        assertThat(prompt).contains("쉼표 뒤 칭호까지 포함한 전체 이름", "모든 전직 노드는 축약하지 말고");
        assertThat(prompt).doesNotContain("mobility");
        assertThat(prompt).doesNotContain("caution", "730000.0", "combinedDps", "\"type\":\"INC\"");
        assertThat(prompt).contains("Secret Weapon", "62% increased Fire Damage", "발동", "자기 피해", "조건", "DPS 순위", "effects");
    }
}
