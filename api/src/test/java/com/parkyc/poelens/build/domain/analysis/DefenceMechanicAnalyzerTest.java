package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefenceMechanicAnalyzerTest {

    private final DefenceMechanicAnalyzer analyzer = new DefenceMechanicAnalyzer();

    @Test
    void createsDistinctResourceAndBlockDefenceLayers() {
        List<Mechanic> result = analyzer.analyse(
                List.of(new DefenceFact("life", 4_500.0), new DefenceFact("block", 40.0)),
                List.of(), List.of(), List.of());

        assertThat(result).extracting(Mechanic::title)
                .contains("생존 자원 기반 방어", "막기 기반 방어");
    }
}
