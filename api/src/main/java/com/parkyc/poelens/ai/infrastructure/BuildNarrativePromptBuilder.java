package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkyc.poelens.build.domain.dto.AscendancyFact;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BuildNarrativePromptBuilder {
    private static final Map<String, String> MECHANICS = Map.of(
            "damage-over-time", "지속 피해는 명중 피해와 별도로 지속 시간 동안 적용됩니다.",
            "trap", "덫 스킬은 덫이 발동할 때 적에게 피해를 줍니다.",
            "armour", "방어도는 물리 피해를 받는 명중의 피해를 줄이는 방어 수치입니다.",
            "block", "막기는 막은 명중의 피해를 막는 방어 수치입니다.",
            "spell-block", "주문 막기는 주문 명중을 막는 방어 수치입니다.",
            "life-regeneration", "생명력 재생은 시간에 따라 생명력을 회복합니다.",
            "spell-suppression", "주문 억제는 주문 명중으로 받는 피해를 줄이는 방어 수치입니다.");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String build(BuildFacts facts) throws Exception {
        Set<String> relevantTags = new LinkedHashSet<>();
        relevantTags.addAll(offence(facts.offence()).stream().flatMap(value -> tags(value.tags()).stream()).toList());
        relevantTags.addAll(offence(facts.offence()).stream().map(OffenceFact::delivery).filter(value -> value != null && !value.isBlank()).toList());
        relevantTags.addAll(defence(facts.defence()).stream().map(DefenceFact::kind).filter(value -> value != null && !value.isBlank()).toList());
        relevantTags.addAll(buffs(facts.buffs()).stream().flatMap(value -> tags(value.tags()).stream()).toList());

        List<PassiveFact> passives = passives(facts.passives()).stream()
                .filter(value -> tags(value.tags()).stream().anyMatch(relevantTags::contains))
                .toList();
        List<AscendancyFact> ascendancies = ascendancies(facts.ascendancies()).stream()
                .filter(value -> tags(value.tags()).stream().anyMatch(relevantTags::contains))
                .toList();
        List<String> mechanics = relevantTags.stream()
                .filter(MECHANICS::containsKey)
                .map(tag -> tag + ": " + MECHANICS.get(tag))
                .toList();
        Set<String> offenceNames = offence(facts.offence()).stream().map(OffenceFact::name).collect(java.util.stream.Collectors.toSet());
        List<SkillSummary> skills = skills(facts.skills()).stream()
                .filter(value -> offenceNames.contains(value.name()))
                .map(value -> new SkillSummary(value.name(), value.level(), supports(value.supports()).stream().map(SupportGemFact::name).toList()))
                .toList();

        Map<String, Object> summary = Map.of(
                "offence", offence(facts.offence()),
                "skills", skills,
                "defence", defence(facts.defence()),
                "buffs", buffs(facts.buffs()),
                "passives", passives,
                "ascendancies", ascendancies,
                "performance", facts.performance());
        return "제공된 PoB 사실과 아래 게임 규칙 참고만 사용해 한국어로 공격과 방어를 각각 한 문장으로 설명해. "
                + "캐릭터에 실제로 존재하지 않는 스킬·수치·효과는 추측하거나 추가하지 마. "
                + "게임 규칙 참고=" + objectMapper.writeValueAsString(mechanics)
                + " PoB 사실 요약=" + objectMapper.writeValueAsString(summary);
    }

    private List<OffenceFact> offence(List<OffenceFact> values) {
        return values == null ? List.of() : values;
    }

    private List<DefenceFact> defence(List<DefenceFact> values) {
        return values == null ? List.of() : values;
    }

    private List<BuffFact> buffs(List<BuffFact> values) {
        return values == null ? List.of() : values;
    }

    private List<PassiveFact> passives(List<PassiveFact> values) {
        return values == null ? List.of() : values;
    }

    private List<AscendancyFact> ascendancies(List<AscendancyFact> values) {
        return values == null ? List.of() : values;
    }

    private List<SkillFact> skills(List<SkillFact> values) {
        return values == null ? List.of() : values;
    }

    private List<SupportGemFact> supports(List<SupportGemFact> values) {
        return values == null ? List.of() : values;
    }

    private List<String> tags(List<String> values) {
        return values == null ? List.of() : values;
    }

    private record SkillSummary(String name, Integer level, List<String> supports) {
    }
}
