package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OffenceMechanicAnalyzer {
    private final MechanicProfileFormatter formatter = new MechanicProfileFormatter();

    public List<Mechanic> analyseNarrative(List<OffenceFact> facts) {
        if (facts == null) return List.of();
        return facts.stream().map(attack -> new Mechanic("공격 기재",
                attack.name() + "의 " + formatter.skillProfile(attack.tags())
                        + " 피해가 확인됩니다. 상세 메커니즘 설명을 생성하지 못했습니다.")).toList();
    }
}
