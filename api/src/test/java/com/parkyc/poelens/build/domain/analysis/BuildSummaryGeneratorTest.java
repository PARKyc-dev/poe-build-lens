package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSummaryGeneratorTest {

    private final BuildSummaryGenerator generator = new BuildSummaryGenerator();

    @Test
    void generatesFallbackSummaryFromAttackDefenceAndBuffFacts() {
        String summary = generator.generate(
                List.of(new OffenceFact("Fire Trap", "primary", 1.0, "trap", List.of("fire"), List.of())),
                List.of(new DefenceFact("armour", 20_000.0)),
                List.of(new BuffFact("Determination", "aura", "player", List.of("armour"))));

        assertThat(summary).isEqualTo("Fire Trap을 주력 공격으로 사용합니다. armour 방어 수치를 기반으로 생존력을 확보합니다. Determination 버프가 빌드 효과를 보강합니다.");
    }
}
