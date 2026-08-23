package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.BuffFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuffMechanicAnalyzerTest {

    private final BuffMechanicAnalyzer analyzer = new BuffMechanicAnalyzer();

    @Test
    void omitsBuffNarrativeWithoutClassifiedEffectTags() {
        assertThat(analyzer.analyse(List.of(new BuffFact("Unclassified", "buff", "player", List.of()))))
                .isEmpty();
    }
}
