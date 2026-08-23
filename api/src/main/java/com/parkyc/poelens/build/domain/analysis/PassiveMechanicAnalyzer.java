package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.AscendancyFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PassiveMechanicAnalyzer {

    private final MechanicProfileFormatter formatter = new MechanicProfileFormatter();

    public List<Mechanic> analyse(List<PassiveFact> passives, List<String> passiveTags) {
        Set<String> tags = new HashSet<>(safe(passiveTags));
        for (PassiveFact passive : safe(passives)) tags.addAll(safe(passive.tags()));
        List<Mechanic> analysis = new ArrayList<>();
        if (formatter.hasAny(tags, "damage-over-time", "fire", "cold", "lightning", "chaos", "physical", "attack", "spell", "minion")) {
            analysis.add(new Mechanic("피해 핵심 패시브", "패시브 효과 태그가 " + formatter.damageProfile(tags) + " 피해 축을 보강합니다."));
        }
        if (formatter.hasAny(tags, "life", "energy-shield", "life-regeneration", "energy-shield-recovery", "armour", "evasion", "ward", "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance")) {
            analysis.add(new Mechanic("생존 핵심 패시브", "패시브 효과 태그가 " + formatter.survivalProfile(tags) + "을 보강합니다."));
        }
        if (formatter.hasAny(tags, "block", "spell-block", "spell-suppression", "attack-dodge", "spell-dodge", "damage-avoidance", "guard")) {
            analysis.add(new Mechanic("적중 방어 핵심 패시브", "패시브 효과 태그가 " + formatter.hitDefenceProfile(tags) + "을 보강합니다."));
        }
        return analysis;
    }

    public List<Mechanic> analyseNodes(List<PassiveFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (PassiveFact fact : safe(facts)) {
            if (fact.name() != null && !fact.name().isBlank()) {
                String effect = safe(fact.effects()).isEmpty() ? "효과 정보가 없습니다." : String.join(" · ", fact.effects());
                analysis.add(new Mechanic("mastery".equals(fact.kind()) ? "마스터리: " + fact.name() : "주요 패시브: " + fact.name(), "적용된 효과: " + effect));
            }
        }
        return analysis;
    }

    public List<Mechanic> analyseAscendancies(List<AscendancyFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (AscendancyFact fact : safe(facts)) {
            if (fact.name() != null && !fact.name().isBlank()) {
                String effect = safe(fact.effects()).isEmpty() ? "효과 정보가 없습니다." : String.join(" · ", fact.effects());
                analysis.add(new Mechanic("전직 노드: " + fact.name(),
                        (fact.ascendancyName() == null ? "전직" : fact.ascendancyName() + " 전직") + "의 적용된 효과: " + effect));
            }
        }
        return analysis;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
