package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.MechanicDetail;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.OperationFact;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import com.parkyc.poelens.build.domain.dto.PerformanceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarrativeSectionValidatorTest {
    private final NarrativeSectionValidator validator = new NarrativeSectionValidator();

    @Test
    void returnsOnlyGroundedAiNarratives() {
        var result = validator.validate(validNarrative(), facts());

        assertThat(result.summary()).isEqualTo("AI가 생성한 빌드 요약입니다.");
        assertThat(result.offence()).containsExactly(new Mechanic("보조젬 연결: Fire Trap", "Burning Damage로 피해를 강화합니다.",
                List.of(new MechanicDetail("Burning Damage", "화상 피해를 강화합니다.", "step"))));
        assertThat(result.defence()).containsExactly(new Mechanic("피해 경감: armour", "방어도로 물리 피해를 줄입니다.",
                List.of(new MechanicDetail("방어도", "방어 구조를 설명합니다.", "interaction"))));
        assertThat(result.buffs()).containsExactly(new Mechanic("방어 버프: Determination", "방어도를 높입니다."));
    }

    @Test
    void rejectsAnIncompleteAiNarrativeInsteadOfUsingRuleBasedText() {
        assertThatThrownBy(() -> validator.validate(Map.of(
                "buildSummary", "AI 요약", "offenceSections", List.of(),
                "defenceSections", List.of(), "buffSections", List.of()), facts()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OpenAI 분석 응답 검증에 실패했습니다.");
    }

    @Test
    void rejectsUnknownEvidence() {
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "modifiers", "explanation", "근거 없는 설명",
                "details", List.of(Map.of("label", "Unknown Modifier", "explanation", "근거가 없습니다.", "type", "interaction")),
                "evidence", List.of("Unknown Modifier"), "flowSubjects", List.of())));

        assertThatThrownBy(() -> validator.validate(narrative, facts()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASectionWithTheWrongDetailType() {
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "supports", "explanation", "보조젬 설명",
                "details", List.of(Map.of("label", "Burning Damage", "explanation", "화상 피해를 강화합니다.", "type", "condition")),
                "evidence", List.of("Burning Damage"), "flowSubjects", List.of())));

        assertThatThrownBy(() -> validator.validate(narrative, facts()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OpenAI 분석 응답 검증에 실패했습니다.");
    }

    @Test
    void allowsAiToSelectTheRelevantDefenceSections() {
        BuildFacts facts = facts();
        BuildFacts withMana = new BuildFacts(facts.offence(), facts.skills(),
                List.of(new DefenceFact("life", 5_000.0), new DefenceFact("energy-shield", 100.0),
                        new DefenceFact("mana", 800.0), new DefenceFact("armour", 21_000.0)),
                facts.buffs(), facts.mobility(), facts.passives(), facts.ascendancies(), facts.passiveTags(),
                facts.items(), facts.jewels(), facts.performance());
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("defenceSections", List.of(Map.of(
                "defenceKind", "life", "section", "resource", "explanation", "생명력을 다른 자원과 함께 설명합니다.",
                "details", List.of(defenceDetail("생명력과 에너지 보호막")),
                "evidence", List.of("energy-shield", "mana", "recovery", "Determination"))));

        assertThat(validator.validate(narrative, withMana).defence())
                .containsExactly(new Mechanic("방어 자원: life", "생명력을 다른 자원과 함께 설명합니다.",
                        List.of(new MechanicDetail("생명력과 에너지 보호막", "방어 구조를 설명합니다.", "interaction"))));
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new OpenAiNarrativeSchema().create(withMana, List.of()));
        assertThat(schema.path("properties").path("defenceSections").path("items").path("properties")
                .path("evidence").path("items").path("enum").toString())
                .contains("energy-shield", "mana", "recovery", "Determination");
    }

    @Test
    void combinesResistancesAndSeparatesTheirCoreInteraction() {
        BuildFacts facts = facts();
        String valako = "Valako, Storm's Embrace";
        BuildFacts resistanceFacts = new BuildFacts(facts.offence(), facts.skills(),
                List.of(new DefenceFact("fire-resistance", 90.0), new DefenceFact("cold-resistance", 90.0),
                        new DefenceFact("lightning-resistance", 90.0), new DefenceFact("chaos-resistance", 75.0)),
                facts.buffs(), facts.mobility(), facts.passives(),
                List.of(new com.parkyc.poelens.build.domain.dto.AscendancyFact("Chieftain", valako,
                        List.of("Modifiers to Maximum Fire Resistance also apply to Maximum Cold and Lightning Resistances"), List.of())),
                facts.passiveTags(), facts.items(), facts.jewels(), facts.performance());
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("defenceSections", List.of(
                Map.of("defenceKind", "resistances", "section", "mitigation", "explanation", "저항 수치를 통합해 설명합니다.",
                        "details", List.of(defenceDetail("원소 저항")),
                        "evidence", List.of("resistances", "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance")),
                Map.of("defenceKind", "resistance-interaction", "section", "mitigation", "explanation", valako + "가 최대 저항을 연결합니다.",
                        "details", List.of(defenceDetail(valako)),
                        "evidence", List.of("resistance-interaction", valako)),
                Map.of("defenceKind", "resistance-interaction", "section", "mitigation", "explanation", "받는 피해의 속성을 전환합니다.",
                        "details", List.of(defenceDetail("피해 전환")),
                        "evidence", List.of("resistance-interaction", valako)),
                Map.of("defenceKind", "resistances", "section", "recovery", "explanation", "점화 피해를 별도로 대응합니다.",
                        "details", List.of(defenceDetail("점화 대응")),
                        "evidence", List.of("resistances", valako))));

        assertThat(validator.validate(narrative, resistanceFacts).defence()).containsExactly(
                new Mechanic("저항 체계", "저항 수치를 통합해 설명합니다.\n\n점화 피해를 별도로 대응합니다.",
                        List.of(new MechanicDetail("원소 저항", "방어 구조를 설명합니다.", "interaction"), new MechanicDetail("점화 대응", "방어 구조를 설명합니다.", "interaction"))),
                new Mechanic("저항 핵심 상호작용", valako + "가 최대 저항을 연결합니다.\n\n받는 피해의 속성을 전환합니다.",
                        List.of(new MechanicDetail(valako, "방어 구조를 설명합니다.", "interaction"), new MechanicDetail("피해 전환", "방어 구조를 설명합니다.", "interaction"))));
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new OpenAiNarrativeSchema().create(resistanceFacts, List.of()));
        assertThat(schema.path("properties").path("defenceSections").path("items").path("properties")
                .path("defenceKind").path("enum").toString())
                .contains("resistances", "resistance-interaction")
                .doesNotContain("fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance");
        assertThat(schema.path("properties").path("defenceSections").path("items").path("properties")
                .path("evidence").path("items").path("enum").toString())
                .contains("resistances", "resistance-interaction", "fire-resistance", valako);
    }

    @Test
    void acceptsAFlowGroundedToTheAttack() {
        OperationFact consume = new OperationFact("skill", "Fire Trap", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFlow flow = new OperationFlow("frenzy-charge", List.of("소비"), List.of(consume));
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "operation", "explanation", "격분 충전을 소비합니다.",
                "details", List.of(Map.of("label", "충전 소비", "explanation", "격분 충전을 소비합니다.", "type", "step")),
                "evidence", List.of("Fire Trap"), "flowSubjects", List.of("frenzy-charge"))));

        var result = validator.validate(narrative, facts(), List.of(flow));

        assertThat(result.offence()).containsExactly(new Mechanic("운용 방식: Fire Trap", "격분 충전을 소비합니다.",
                List.of(new MechanicDetail("충전 소비", "격분 충전을 소비합니다.", "step"))));
    }

    @Test
    void schemaRestrictsAttackNamesAndEvidenceToExistingSources() {
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new OpenAiNarrativeSchema().create(facts(), List.of()));
        var properties = schema.path("properties").path("offenceSections").path("items").path("anyOf").get(0).path("properties");

        assertThat(properties.path("attackName").path("enum").toString()).isEqualTo("[\"Fire Trap\"]");
        assertThat(properties.path("evidence").path("items").path("enum").toString())
                .contains("Fire Trap", "Burning Damage", "Determination");
        assertThat(schema.path("properties").path("defenceSections").path("items").path("properties")
                .path("defenceKind").path("enum").toString()).isEqualTo("[\"armour\"]");
    }

    @Test
    void requiresNoBuffSectionsWhenNoBuffHasAnalysisTags() {
        BuildFacts facts = facts();
        BuildFacts withoutClassifiedBuffs = new BuildFacts(facts.offence(), facts.skills(), facts.defence(),
                List.of(new BuffFact("Hatred", "aura", "player", List.of())), facts.mobility(), facts.passives(),
                facts.ascendancies(), facts.passiveTags(), facts.items(), facts.jewels(), facts.performance());
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("defenceSections", List.of(Map.of(
                "defenceKind", "armour", "section", "mitigation", "explanation", "방어도로 물리 피해를 줄입니다.",
                "details", List.of(defenceDetail("방어도")),
                "evidence", List.of("armour"))));
        narrative.put("buffSections", List.of());

        assertThat(validator.validate(narrative, withoutClassifiedBuffs).buffs()).isEmpty();
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                new OpenAiNarrativeSchema().create(withoutClassifiedBuffs, List.of()));
        var buffSections = schema.path("properties").path("buffSections");
        assertThat(buffSections.path("maxItems").asInt(-1)).isZero();
        assertThat(buffSections.path("items").path("additionalProperties").asBoolean()).isFalse();
        assertThat(buffSections.path("items").path("required").size()).isEqualTo(4);
    }

    @Test
    void allowsBuffSectionsToUseOtherGroundedBuildSources() {
        Map<String, Object> narrative = new java.util.HashMap<>(validNarrative());
        narrative.put("buffSections", List.of(Map.of(
                "buffName", "Determination", "section", "defence", "explanation", "스킬과 함께 작동합니다.",
                "evidence", List.of("Fire Trap"))));

        assertThat(validator.validate(narrative, facts()).buffs())
                .containsExactly(new Mechanic("방어 버프: Determination", "스킬과 함께 작동합니다."));
    }

    @Test
    void acceptsGroundedAnalysisWhenAnOptionalAttackSectionIsOmitted() {
        BuildFacts facts = facts();
        BuildFacts withAnotherAttack = new BuildFacts(
                List.of(facts.offence().getFirst(), new OffenceFact("Flame Wall", "secondary", 1.0,
                        "self-cast", List.of(), List.of())),
                facts.skills(), facts.defence(), facts.buffs(), facts.mobility(), facts.passives(), facts.ascendancies(),
                facts.passiveTags(), facts.items(), facts.jewels(), facts.performance());

        assertThat(validator.validate(validNarrative(), withAnotherAttack).offence())
                .containsExactly(new Mechanic("보조젬 연결: Fire Trap", "Burning Damage로 피해를 강화합니다.",
                        List.of(new MechanicDetail("Burning Damage", "화상 피해를 강화합니다.", "step"))));
    }

    @Test
    void schemaRequiresEmptyOffenceSectionsWhenThereAreNoAttacks() {
        BuildFacts facts = facts();
        BuildFacts withoutAttacks = new BuildFacts(List.of(), facts.skills(), facts.defence(), facts.buffs(),
                facts.mobility(), facts.passives(), facts.ascendancies(), facts.passiveTags(), facts.items(),
                facts.jewels(), facts.performance());
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                new OpenAiNarrativeSchema().create(withoutAttacks, List.of()));

        var offenceSections = schema.path("properties").path("offenceSections");
        assertThat(offenceSections.path("maxItems").asInt(-1)).isZero();
        assertThat(offenceSections.path("items").path("additionalProperties").asBoolean()).isFalse();
        assertThat(offenceSections.path("items").path("required").size()).isEqualTo(6);
    }

    private Map<String, Object> validNarrative() {
        return Map.of(
                "buildSummary", "AI가 생성한 빌드 요약입니다.",
                "offenceSections", List.of(Map.of(
                        "attackName", "Fire Trap", "section", "supports", "explanation", "Burning Damage로 피해를 강화합니다.",
                        "details", List.of(Map.of("label", "Burning Damage", "explanation", "화상 피해를 강화합니다.", "type", "step")),
                        "evidence", List.of("Burning Damage"), "flowSubjects", List.of())),
                "defenceSections", List.of(Map.of(
                        "defenceKind", "armour", "section", "mitigation", "explanation", "방어도로 물리 피해를 줄입니다.",
                        "details", List.of(defenceDetail("방어도")),
                        "evidence", List.of("armour", "Determination"))),
                "buffSections", List.of(Map.of(
                        "buffName", "Determination", "section", "defence", "explanation", "방어도를 높입니다.",
                        "evidence", List.of("Determination"))));
    }

    private Map<String, Object> defenceDetail(String label) {
        return Map.of("label", label, "explanation", "방어 구조를 설명합니다.", "type", "interaction");
    }

    private BuildFacts facts() {
        return new BuildFacts(
                List.of(new OffenceFact("Fire Trap", "primary", 1.0, "trap", List.of(),
                        List.of(new AppliedModifierFact("Fire Damage", "INC", "Passive", false)))),
                List.of(new SkillFact("Fire Trap", 20, 0, "Default", true, false,
                        List.of(new SupportGemFact("Burning Damage", 20, 0, "Default", true, false, List.of())))),
                List.of(new DefenceFact("armour", 21_000.0)),
                List.of(new BuffFact("Determination", "aura", "player", List.of("armour"))),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                new PerformanceFact(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }
}
