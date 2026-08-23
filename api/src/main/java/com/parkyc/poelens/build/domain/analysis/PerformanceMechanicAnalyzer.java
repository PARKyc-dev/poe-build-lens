package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.PerformanceFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PerformanceMechanicAnalyzer {

    public List<Mechanic> analyse(PerformanceFact fact) {
        if (fact == null) return List.of();
        List<String> values = new ArrayList<>();
        addPerformance(values, "주력 DPS", fact.totalDps());
        addPerformance(values, "합산 DPS", fact.combinedDps());
        addPerformance(values, "생명력", fact.life());
        addPerformance(values, "에너지 보호막", fact.energyShield());
        addPerformance(values, "마나", fact.mana());
        addPerformance(values, "방어도", fact.armour());
        addPerformance(values, "회피", fact.evasion());
        addPerformance(values, "총 EHP", fact.totalEhp());
        return values.isEmpty() ? List.of() : List.of(new Mechanic("PoB 계산 수치", String.join(" · ", values)));
    }

    private void addPerformance(List<String> values, String label, Double value) {
        if (value != null) values.add(label + " " + String.format("%,d", Math.round(value)));
    }
}
