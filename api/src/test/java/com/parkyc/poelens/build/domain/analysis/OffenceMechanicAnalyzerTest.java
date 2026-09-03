package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OffenceMechanicAnalyzerTest {

    private final OffenceMechanicAnalyzer analyzer = new OffenceMechanicAnalyzer();

    @Test
    void doesNotPresentRawModifiersOrGenericOperationAsAnAnalysis() {
        List<Mechanic> result = analyzer.analyseNarrative(
                List.of(new OffenceFact("Fire Trap", "primary", 1.0, "trap", List.of("fire"),
                        List.of(new AppliedModifierFact("Fire Damage", "INC", "Passive", true)))));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().explanation()).contains("Fire Trap", "상세 메커니즘 설명을 생성하지 못했습니다");
        assertThat(result.toString()).doesNotContain("Supports any", "INC", "조건부 효과는", "주력으로");
    }
}
