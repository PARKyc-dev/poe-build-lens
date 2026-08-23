package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
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
                        "evidence", List.of("Fire Trap"))),
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
                        Map.of("attackName", "Fire Trap", "section", "core", "explanation", "덫을 설치합니다.", "evidence", List.of("Fire Trap")),
                        Map.of("attackName", "Flame Wall", "section", "operation", "explanation", "직접 시전합니다.", "evidence", List.of("Flame Wall"))),
                "defenceSections", List.of(),
                "buffSections", List.of()), factsWithTwoAttacks(), fallback);

        assertThat(result.offence()).containsExactly(
                new Mechanic("핵심 동작: Fire Trap", "덫을 설치합니다."),
                new Mechanic("운용 방식: Flame Wall", "직접 시전합니다."));
    }

    @Test
    void usesStructuredOffenceSectionWhenItsEvidenceBelongsToTheAttack() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "supports", "explanation", "Burning Damage로 피해를 강화합니다.",
                "evidence", List.of("Fire Trap", "Burning Damage")))), facts(), fallback);

        assertThat(result.offence()).containsExactly(new Mechanic("보조젬 연결: Fire Trap", "Burning Damage로 피해를 강화합니다."));
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionReferencesUnknownEvidence() {
        NarrativeResult result = validator.validate(Map.of("offenceSections", List.of(Map.of(
                "attackName", "Fire Trap", "section", "modifiers", "explanation", "존재하지 않는 효과를 사용합니다.",
                "evidence", List.of("Unknown Modifier")))), facts(), fallback);

        assertThat(result.offence()).isEqualTo(fallback.offence());
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
