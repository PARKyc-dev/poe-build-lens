package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record BuildFacts(
        List<OffenceFact> offence,
        List<SkillFact> skills,
        List<DefenceFact> defence,
        List<BuffFact> buffs,
        List<MobilityFact> mobility,
        List<PassiveFact> passives,
        List<AscendancyFact> ascendancies,
        List<String> passiveTags,
        List<ItemFact> items,
        List<JewelFact> jewels,
        PerformanceFact performance) {
}
