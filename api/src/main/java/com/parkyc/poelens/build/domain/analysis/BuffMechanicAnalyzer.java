package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BuffMechanicAnalyzer {

    private final MechanicProfileFormatter formatter = new MechanicProfileFormatter();

    public List<Mechanic> analyse(List<BuffFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (BuffFact fact : safe(facts)) {
            Set<String> tags = new HashSet<>(safe(fact.tags()));
            if (tags.isEmpty()) continue;
            analysis.add(new Mechanic("버프 유틸리티: " + fact.name(), formatter.utilityProfile(tags) + " 태그가 활성화되어 " + formatter.utilityEffect(tags) + "를 보강합니다."));
        }
        return analysis;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
