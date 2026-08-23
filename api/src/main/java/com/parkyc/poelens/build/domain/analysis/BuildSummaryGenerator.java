package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BuildSummaryGenerator {

    public String generate(List<OffenceFact> offence, List<DefenceFact> defence, List<BuffFact> buffs) {
        List<String> parts = new ArrayList<>();
        List<String> attacks = safe(offence).stream().filter(fact -> "primary".equals(fact.role())).map(OffenceFact::name).filter(name -> name != null && !name.isBlank()).toList();
        if (!attacks.isEmpty()) parts.add(String.join("·", attacks) + "을 주력 공격으로 사용합니다.");
        List<String> defenceKinds = safe(defence).stream().filter(fact -> fact.value() != null && fact.value() > 0).map(DefenceFact::kind).filter(kind -> kind != null && !kind.isBlank()).toList();
        if (!defenceKinds.isEmpty()) parts.add(String.join("·", defenceKinds) + " 방어 수치를 기반으로 생존력을 확보합니다.");
        List<String> buffNames = safe(buffs).stream().map(BuffFact::name).filter(name -> name != null && !name.isBlank()).distinct().toList();
        if (!buffNames.isEmpty()) parts.add(String.join("·", buffNames) + " 버프가 빌드 효과를 보강합니다.");
        return parts.isEmpty() ? "PoB에 기록된 공격·방어·버프 사실을 기준으로 분석한 빌드입니다." : String.join(" ", parts);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
