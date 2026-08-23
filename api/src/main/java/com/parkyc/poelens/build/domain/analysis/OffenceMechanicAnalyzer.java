package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OffenceMechanicAnalyzer {

    private final MechanicProfileFormatter formatter = new MechanicProfileFormatter();

    public List<Mechanic> analyseNarrative(List<OffenceFact> facts, List<SkillFact> skills) {
        List<OffenceFact> attacks = safe(facts);
        if (attacks.isEmpty()) return List.of();
        List<Mechanic> analysis = new ArrayList<>();
        for (OffenceFact attack : attacks) {
            String role = "secondary".equals(attack.role()) ? "보조" : "주력";
            analysis.add(new Mechanic("primary".equals(attack.role()) ? "공격 기재" : "보조 공격 기재", attack.name() + "이 " + formatter.skillProfile(attack.tags()) + " 피해를 " + role + "으로 담당합니다."));
            safe(skills).stream().filter(skill -> attack.name().equals(skill.name())).findFirst().ifPresent(skill -> {
                List<String> supports = safe(skill.supports()).stream().filter(support -> Boolean.TRUE.equals(support.enabled())).map(support -> support.name() + (safe(support.effects()).isEmpty() ? "" : ": " + String.join(" ", support.effects()))).toList();
                if (!supports.isEmpty()) analysis.add(new Mechanic("보조젬 연결: " + attack.name(), String.join(" · ", supports)));
            });
            List<String> applied = safe(attack.modifiers()).stream().map(modifier -> modifier.source() + "의 " + modifier.name() + " (" + modifier.type() + ")" + (Boolean.TRUE.equals(modifier.conditional()) ? " · 조건부" : "")).distinct().limit(12).toList();
            if (!applied.isEmpty()) analysis.add(new Mechanic("적용된 빌드 효과: " + attack.name(), String.join(" · ", applied)));
            analysis.add(new Mechanic("운용 방식: " + attack.name(), formatter.deliveryTitle(attack.delivery()) + " 방식으로 사용합니다. 조건부 효과는 PoB 설정에서 활성화된 경우에만 계산에 반영됩니다."));
        }
        return analysis;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
