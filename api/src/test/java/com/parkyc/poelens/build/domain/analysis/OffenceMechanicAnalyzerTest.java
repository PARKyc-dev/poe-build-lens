package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.AppliedModifierFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OffenceMechanicAnalyzerTest {

    private final OffenceMechanicAnalyzer analyzer = new OffenceMechanicAnalyzer();

    @Test
    void createsCoreSupportModifierAndOperationSectionsForPrimaryAttack() {
        List<Mechanic> result = analyzer.analyseNarrative(
                List.of(new OffenceFact("Fire Trap", "primary", 1.0, "trap", List.of("fire"),
                        List.of(new AppliedModifierFact("Fire Damage", "INC", "Passive", true)))),
                List.of(new SkillFact("Fire Trap", 20, 0, "Default", true, false,
                        List.of(new SupportGemFact("Burning Damage", 20, 0, "Default", true, false,
                                List.of("Supports any skill that deals damage."))))));

        assertThat(result).containsExactly(
                new Mechanic("공격 기재", "Fire Trap이 화염 피해를 주력으로 담당합니다."),
                new Mechanic("보조젬 연결: Fire Trap", "Burning Damage: Supports any skill that deals damage."),
                new Mechanic("적용된 빌드 효과: Fire Trap", "Passive의 Fire Damage (INC) · 조건부"),
                new Mechanic("운용 방식: Fire Trap", "트랩 (Trap) 방식으로 사용합니다. 조건부 효과는 PoB 설정에서 활성화된 경우에만 계산에 반영됩니다."));
    }
}
