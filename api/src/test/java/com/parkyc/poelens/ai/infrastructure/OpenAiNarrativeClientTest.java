package com.parkyc.poelens.ai.infrastructure;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.PerformanceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiNarrativeClientTest {
    private final List<Mechanic> fallback = List.of(new Mechanic("공격 기재", "규칙 기반 설명"));

    @Test
    void usesStructuredOffenceSectionWhenItsEvidenceBelongsToTheAttack() {
        List<Mechanic> result = OpenAiNarrativeClient.offenceSections(List.of(Map.of(
                "attackName", "Fire Trap", "section", "supports", "explanation", "Burning Damage로 피해를 강화합니다.",
                "evidence", List.of("Fire Trap", "Burning Damage"))), facts(), fallback);

        assertThat(result).containsExactly(new Mechanic("보조젬 연결: Fire Trap", "Burning Damage로 피해를 강화합니다."));
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionReferencesUnknownEvidence() {
        List<Mechanic> result = OpenAiNarrativeClient.offenceSections(List.of(Map.of(
                "attackName", "Fire Trap", "section", "modifiers", "explanation", "존재하지 않는 효과를 사용합니다.",
                "evidence", List.of("Unknown Modifier"))), facts(), fallback);

        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionsOmitAnAttack() {
        List<Mechanic> result = OpenAiNarrativeClient.offenceSections(List.of(Map.of(
                "attackName", "Fire Trap", "section", "core", "explanation", "덫을 설치합니다.",
                "evidence", List.of("Fire Trap"))), factsWithTwoAttacks(), fallback);

        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void fallsBackWhenStructuredOffenceSectionUsesRemovedCautionKind() {
        List<Mechanic> result = OpenAiNarrativeClient.offenceSections(List.of(Map.of(
                "attackName", "Fire Trap", "section", "caution", "explanation", "주의 문장", "evidence", List.of("Fire Trap"))), facts(), fallback);

        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void usesStructuredDefenceSectionWhenItReferencesItsDefenceKind() {
        List<Mechanic> fallbackDefence = List.of(new Mechanic("방어 기재", "규칙 기반 방어 설명"));

        List<Mechanic> result = OpenAiNarrativeClient.defenceSections(List.of(Map.of(
                "defenceKind", "armour", "section", "mitigation", "explanation", "방어도로 물리 적중 피해를 줄입니다.",
                "evidence", List.of("armour"))), facts(), fallbackDefence);

        assertThat(result).containsExactly(new Mechanic("피해 경감: armour", "방어도로 물리 적중 피해를 줄입니다."));
    }

    @Test
    void fallsBackWhenStructuredBuffSectionReferencesAnotherBuff() {
        List<Mechanic> fallbackBuffs = List.of(new Mechanic("버프 기재", "규칙 기반 버프 설명"));

        List<Mechanic> result = OpenAiNarrativeClient.buffSections(List.of(Map.of(
                "buffName", "Determination", "section", "defence", "explanation", "방어도를 높입니다.",
                "evidence", List.of("Hatred"))), facts(), fallbackBuffs);

        assertThat(result).isEqualTo(fallbackBuffs);
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
}
