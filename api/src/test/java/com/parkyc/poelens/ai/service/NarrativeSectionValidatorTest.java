package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
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

class NarrativeSectionValidatorTest {
    private final NarrativeSectionValidator validator = new NarrativeSectionValidator();
    private final NarrativeResult fallback = new NarrativeResult(
            "규칙 기반 요약",
            List.of(new Mechanic("공격 기재", "규칙 기반 공격 설명")),
            List.of(new Mechanic("방어 기재", "규칙 기반 방어 설명")),
            List.of(new Mechanic("버프 기재", "규칙 기반 버프 설명")));

    @Test
    void usesValidatedSectionsOnlyWhenEveryAttackIsCovered() {
        NarrativeResult result = validator.validate(Map.of(
                "buildSummary", "AI 요약",
                "offenceSections", List.of(Map.of(
                        "attackName", "Fire Trap", "section", "core", "explanation", "덫을 설치합니다.",
                        "evidence", List.of("Fire Trap"), "flowSubjects", List.of())),
                "defenceSections", List.of(),
                "buffSections", List.of()), factsWithTwoAttacks(), fallback);

        assertThat(result.summary()).isEqualTo("AI 요약");
        assertThat(result.offence()).isEqualTo(fallback.offence());
        assertThat(result.defence()).isEqualTo(fallback.defence());
        assertThat(result.buffs()).isEqualTo(fallback.buffs());
    }

    @Test
    void usesGroundedSectionsWhenEverySubjectIsCovered() {
        NarrativeResult result = validator.validate(Map.of(
                "buildSummary", "AI 요약",
                "offenceSections", List.of(
                        Map.of("attackName", "Fire Trap", "section", "core", "explanation", "덫을 설치합니다.", "evidence", List.of("Fire Trap"), "flowSubjects", List.of()),
                        Map.of("attackName", "Flame Wall", "section", "core", "explanation", "직접 시전합니다.", "evidence", List.of("Flame Wall"), "flowSubjects", List.of())),
                "defenceSections", List.of(),
                "buffSections", List.of()), factsWithTwoAttacks(), fallback);

        assertThat(result.offence()).containsExactly(
                new Mechanic("공격이 작동하는 과정: Fire Trap", "덫을 설치합니다."),
                new Mechanic("공격이 작동하는 과정: Flame Wall", "직접 시전합니다."));
    }

    @Test
    void usesStructuredOffenceSectionWhenItsEvidenceBelongsToTheAttack() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "supports", "explanation", "Burning Damage로 피해를 강화합니다.",
                "evidence", List.of("Fire Trap", "Burning Damage"), "flowSubjects", List.of()))), facts(), fallback);

        assertThat(result.offence()).containsExactly(new Mechanic("보조젬 연결: Fire Trap", "Burning Damage로 피해를 강화합니다."));
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionReferencesUnknownEvidence() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "modifiers", "explanation", "존재하지 않는 효과를 사용합니다.",
                "evidence", List.of("Unknown Modifier"), "flowSubjects", List.of()))), facts(), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void fallsBackWhenOffenceSectionReferencesUnknownFlowSubject() {
        OperationFact consumesFrenzy = new OperationFact("skill", "Fire Trap", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFact gainsFrenzy = new OperationFact("item", "Generic Item", "gain", "frenzy-charge", List.of("Gain a Frenzy Charge on Hit"));
        OperationFlow frenzyFlow = new OperationFlow("frenzy-charge", List.of("frenzy-charge 소비", "frenzy-charge 획득", "연속 사용"), List.of(consumesFrenzy, gainsFrenzy));

        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "operation", "explanation", "근거 없는 흐름을 설명합니다.",
                "evidence", List.of("Fire Trap"), "flowSubjects", List.of("unknown-flow")))), facts(), List.of(frenzyFlow), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void fallsBackWhenOperationSectionOmitsFlowSubjects() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "operation", "explanation", "근거 없는 운용을 설명합니다.",
                "evidence", List.of("Fire Trap"), "flowSubjects", List.of()))), facts(), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void fallsBackWhenFlowIsGroundedToAnotherAttack() {
        OperationFact consumesFrenzy = new OperationFact("skill", "Other Attack", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFact gainsFrenzy = new OperationFact("item", "Generic Item", "gain", "frenzy-charge", List.of("Gain a Frenzy Charge on Hit"));
        OperationFlow frenzyFlow = new OperationFlow("frenzy-charge", List.of("frenzy-charge 소비", "frenzy-charge 획득", "연속 사용"), List.of(consumesFrenzy, gainsFrenzy));

        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "operation", "explanation", "다른 공격의 흐름을 설명합니다.",
                "evidence", List.of("Fire Trap"), "flowSubjects", List.of("frenzy-charge")))), facts(), List.of(frenzyFlow), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void usesOperationSectionWhenFlowIsGroundedToItsPrimaryAttack() {
        OperationFact consumesFrenzy = new OperationFact("skill", "Fire Trap", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFact gainsFrenzy = new OperationFact("item", "Generic Item", "gain", "frenzy-charge", List.of("Gain a Frenzy Charge on Hit"));
        OperationFlow frenzyFlow = new OperationFlow("frenzy-charge", List.of("frenzy-charge 소비", "frenzy-charge 획득", "연속 사용"), List.of(consumesFrenzy, gainsFrenzy));

        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "operation", "explanation", "충전을 소비하고 다시 획득해 연속 사용합니다.",
                "evidence", List.of("Fire Trap"), "flowSubjects", List.of("frenzy-charge")))), facts(), List.of(frenzyFlow), fallback);

        assertThat(result.offence()).containsExactly(new Mechanic("운용 방식: Fire Trap", "충전을 소비하고 다시 획득해 연속 사용합니다."));
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionUsesRemovedCautionKind() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "caution", "explanation", "주의 문장", "evidence", List.of("Fire Trap")))), facts(), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void usesStructuredDefenceSectionWhenItReferencesItsDefenceKind() {
        NarrativeResult result = validator.validate(Map.of("defenceSections", List.of(Map.of(
                "defenceKind", "armour", "section", "mitigation", "explanation", "방어도로 물리 적중 피해를 줄입니다.",
                "evidence", List.of("armour")))), facts(), fallback);

        assertThat(result.defence()).containsExactly(new Mechanic("피해 경감: armour", "방어도로 물리 적중 피해를 줄입니다."));
    }

    @Test
    void fallsBackWhenStructuredBuffSectionReferencesAnotherBuff() {
        NarrativeResult result = validator.validate(Map.of("buffSections", List.of(Map.of(
                "buffName", "Determination", "section", "defence", "explanation", "방어도를 높입니다.",
                "evidence", List.of("Hatred")))), facts(), fallback);

        assertThat(result.buffs()).isEqualTo(fallback.buffs());
    }

    @Test
    void omitsAiBuffSectionWhenPobDoesNotProvideEffectTags() {
        NarrativeResult result = validator.validate(Map.of("buffSections", List.of(Map.of(
                "buffName", "Unclassified Aura", "section", "utility", "explanation", "플레이어에게 활성화되어 있습니다.",
                "evidence", List.of("Unclassified Aura")))), factsWithUnclassifiedBuff(),
                new NarrativeResult("규칙 기반 요약", List.of(), List.of(), List.of()));

        assertThat(result.buffs()).isEmpty();
    }

    @Test
    void acceptsCrossSkillInteractionAndEffectsBasedOperationForSecondaryAttack() {
        BuildFacts original = facts();
        BuildFacts facts = new BuildFacts(
                List.of(new OffenceFact("Fire Trap", "secondary", 1.0, "trap", List.of(), List.of())),
                List.of(new SkillFact("Fire Trap", 20, 0, "Default", true, false, List.of(), List.of("Leaves burning ground")),
                        new SkillFact("Shield Charge", 20, 0, "Default", true, false,
                                List.of(new SupportGemFact("Lifetap", 1, 0, "Default", true, false, List.of("Spend Life to gain Lifetap"))))),
                original.defence(), original.buffs(), List.of(), List.of(), List.of(), List.of(),
                List.of(new com.parkyc.poelens.build.domain.dto.ItemFact("Ring 1", "Death Rush", "Amethyst Ring", "UNIQUE", List.of("Gain Adrenaline on Kill"), List.of())),
                List.of(), original.performance());
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(
                Map.of("attackName", "Fire Trap", "section", "modifiers", "explanation", "다른 스킬과 장비의 조건부 효과를 연결합니다.",
                        "evidence", List.of("Fire Trap", "Shield Charge", "Lifetap", "Death Rush"), "flowSubjects", List.of()),
                Map.of("attackName", "Fire Trap", "section", "operation", "explanation", "적 위치에 덫을 던져 불타는 지대를 만듭니다.",
                        "evidence", List.of("Fire Trap"), "flowSubjects", List.of()))), facts, fallback);
        assertThat(result.offence()).hasSize(2);
        assertThat(result.offence().getFirst().title()).isEqualTo("핵심 상호작용: Fire Trap");
        assertThat(result.offence().get(1).title()).isEqualTo("운용 방식: Fire Trap");
    }

    @Test
    void rejectsInteractionWithoutItsAttackReference() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "modifiers", "explanation", "버프 설명",
                "evidence", List.of("Determination"), "flowSubjects", List.of()))), facts(), fallback);
        assertThat(result.offence()).isEqualTo(fallback.offence());
    }

    @Test
    void schemaRestrictsAttackNamesAndEvidenceToExistingSources() {
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new OpenAiNarrativeSchema().create(facts(), List.of()));
        var properties = schema.path("properties").path("offenceSections").path("items").path("anyOf").get(0).path("properties");
        assertThat(properties.path("attackName").path("enum").toString()).isEqualTo("[\"Fire Trap\"]");
        assertThat(properties.path("evidence").path("items").path("enum").toString()).contains("Fire Trap", "Burning Damage", "Determination");
        assertThat(properties.path("flowSubjects").path("maxItems").asInt()).isZero();
    }

    @Test
    void schemaDoesNotOfferAnotherAttacksFlowSubjects() {
        OperationFlow flow = new OperationFlow("Fire Trap", List.of("처치", "후속 효과"), List.of());
        var schema = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                new OpenAiNarrativeSchema().create(factsWithTwoAttacks(), List.of(flow)));
        var variants = schema.path("properties").path("offenceSections").path("items").path("anyOf");
        assertThat(variants.get(0).path("properties").path("flowSubjects").path("items").path("enum").toString())
                .isEqualTo("[\"Fire Trap\"]");
        assertThat(variants.get(1).path("properties").path("flowSubjects").path("maxItems").asInt(-1)).isZero();
    }

    private BuildFacts facts() {
        return new BuildFacts(
                List.of(new OffenceFact("Fire Trap", "primary", 1.0, "trap", List.of(), List.of(new AppliedModifierFact("Fire Damage", "INC", "Passive", false)))),
                List.of(new SkillFact("Fire Trap", 20, 0, "Default", true, false, List.of(new SupportGemFact("Burning Damage", 20, 0, "Default", true, false, List.of())))),
                List.of(new DefenceFact("armour", 21_000.0)), List.of(new BuffFact("Determination", "aura", "player", List.of("armour"))), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                new PerformanceFact(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }

    private BuildFacts factsWithTwoAttacks() {
        BuildFacts facts = facts();
        return new BuildFacts(
                List.of(facts.offence().getFirst(), new OffenceFact("Flame Wall", "secondary", 1.0, "self-cast", List.of(), List.of())),
                facts.skills(), facts.defence(), facts.buffs(), facts.mobility(), facts.passives(), facts.ascendancies(), facts.passiveTags(), facts.items(), facts.jewels(), facts.performance());
    }

    private BuildFacts factsWithUnclassifiedBuff() {
        BuildFacts facts = facts();
        return new BuildFacts(
                facts.offence(), facts.skills(), facts.defence(), List.of(new BuffFact("Unclassified Aura", "aura", "player", List.of())),
                facts.mobility(), facts.passives(), facts.ascendancies(), facts.passiveTags(), facts.items(), facts.jewels(), facts.performance());
    }
}
