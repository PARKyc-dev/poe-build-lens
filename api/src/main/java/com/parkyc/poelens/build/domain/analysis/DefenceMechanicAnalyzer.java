package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.ItemFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DefenceMechanicAnalyzer {

    private final MechanicProfileFormatter formatter = new MechanicProfileFormatter();

    public List<Mechanic> analyse(List<DefenceFact> facts, List<PassiveFact> passives, List<String> passiveTags, List<ItemFact> items) {
        List<Mechanic> analysis = new ArrayList<>();
        Set<String> kinds = safe(facts).stream()
                .filter(fact -> fact.value() != null && fact.value() > 0)
                .map(DefenceFact::kind)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> supportTags = supportTags(passives, passiveTags, items);
        if (formatter.hasAny(kinds, "life", "energy-shield")) {
            analysis.add(new Mechanic("생존 자원 기반 방어",
                    formatter.resourceProfile(kinds) + "을 피해를 견디는 기본 자원으로 사용합니다." + formatter.supportSuffix(supportTags, "life", "energy-shield", "life-regeneration", "energy-shield-recovery")));
        }
        if (kinds.containsAll(Set.of("fire-resistance", "cold-resistance", "lightning-resistance"))) {
            analysis.add(new Mechanic("원소 저항 기반 방어",
                    "화염·냉기·번개 저항 수치로 원소 피해를 줄입니다." + formatter.supportSuffix(supportTags, "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance")));
        }
        if (formatter.hasAny(kinds, "armour", "evasion", "physical-mitigation")) {
            analysis.add(new Mechanic("방어도·회피 기반 방어",
                    formatter.defenceProfile(kinds, "armour", "방어도", "evasion", "회피", "physical-mitigation", "물리 피해 감소") + "로 물리 적중 피해를 줄이거나 피합니다." + formatter.supportSuffix(supportTags, "armour", "evasion", "physical-mitigation")));
        }
        if (formatter.hasAny(kinds, "block", "spell-block")) {
            analysis.add(new Mechanic("막기 기반 방어",
                    formatter.defenceProfile(kinds, "block", "공격 막기", "spell-block", "주문 막기") + "로 적중 피해를 막습니다." + formatter.supportSuffix(supportTags, "block", "spell-block")));
        }
        if (kinds.contains("spell-suppression")) {
            analysis.add(new Mechanic("주문 방어",
                    "주문 억제 수치로 주문 적중 피해를 줄입니다." + formatter.supportSuffix(supportTags, "spell-suppression")));
        }
        if (formatter.hasAny(kinds, "ward", "guard", "attack-dodge", "spell-dodge", "damage-avoidance")) {
            analysis.add(new Mechanic("보조 피해 방어",
                    formatter.defenceProfile(kinds, "ward", "와드", "guard", "가드", "attack-dodge", "공격 회피", "spell-dodge", "주문 회피", "damage-avoidance", "피해 회피") + "로 추가 피해 방어 층을 구성합니다." + formatter.supportSuffix(supportTags, "ward", "guard", "attack-dodge", "spell-dodge", "damage-avoidance")));
        }
        return analysis;
    }

    private Set<String> supportTags(List<PassiveFact> passives, List<String> passiveTags, List<ItemFact> items) {
        Set<String> supportTags = new HashSet<>(safe(passiveTags));
        for (PassiveFact passive : safe(passives)) supportTags.addAll(safe(passive.tags()));
        for (ItemFact item : safe(items)) supportTags.addAll(safe(item.tags()));
        return supportTags;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
