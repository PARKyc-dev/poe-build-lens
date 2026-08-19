package com.parkyc.poelens.ai.infrastructure;

import com.parkyc.poelens.build.domain.dto.AscendancyFact;
import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.ItemFact;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import com.parkyc.poelens.build.domain.dto.PerformanceFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildNarrativePromptBuilderTest {

    @Test
    void includesRelevantMechanicsAndExcludesRawEquipmentDetails() throws Exception {
        BuildFacts facts = new BuildFacts(
                List.of(new OffenceFact("Fire Trap", "primary", 730_000.0, "trap", List.of("fire", "damage-over-time"))),
                List.of(new SkillFact("Fire Trap", 21, 20, "Default", true, false, List.of())),
                List.of(new DefenceFact("armour", 21_000.0), new DefenceFact("block", 70.0)),
                List.of(new BuffFact("Determination", "aura", "player", List.of("armour"))),
                List.of(),
                List.of(new PassiveFact("Arsonist", List.of("Regenerate 1.2% of Life per second"), List.of("life-regeneration"))),
                List.of(new AscendancyFact("Chieftain", "Hinekora, Death's Fury", List.of("Enemies you kill have a chance to Explode"), List.of("fire"))),
                List.of("armour", "life-regeneration"),
                List.of(new ItemFact("Weapon 1", "Secret Weapon", "Void Sceptre", "RARE", List.of("62% increased Fire Damage"), List.of("fire"))),
                List.of(),
                new PerformanceFact(0.0, 730_000.0, 5_800.0, 0.0, 800.0, 21_000.0, 0.0, 100_000.0));

        String prompt = new BuildNarrativePromptBuilder().build(facts);

        assertThat(prompt).contains("지속 피해", "덫", "방어도", "막기", "Fire Trap", "730000.0", "Determination", "Hinekora, Death's Fury");
        assertThat(prompt).doesNotContain("Secret Weapon", "62% increased Fire Damage");
    }
}
