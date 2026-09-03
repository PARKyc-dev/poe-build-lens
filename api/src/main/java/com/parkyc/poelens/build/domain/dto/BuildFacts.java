package com.parkyc.poelens.build.domain.dto;

import java.util.List;
import java.util.Map;

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
        List<OperationFact> operationFacts,
        PerformanceFact performance,
        Map<String, Object> conditions) {

    public BuildFacts(List<OffenceFact> offence, List<SkillFact> skills, List<DefenceFact> defence,
                      List<BuffFact> buffs, List<MobilityFact> mobility, List<PassiveFact> passives,
                      List<AscendancyFact> ascendancies, List<String> passiveTags, List<ItemFact> items,
                      List<JewelFact> jewels, List<OperationFact> operationFacts, PerformanceFact performance) {
        this(offence, skills, defence, buffs, mobility, passives, ascendancies, passiveTags, items, jewels,
                operationFacts, performance, Map.of());
    }

    public BuildFacts(
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
        this(offence, skills, defence, buffs, mobility, passives, ascendancies, passiveTags, items, jewels, null, performance);
    }
}
