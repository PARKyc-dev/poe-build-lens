package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record BuildFacts(
        List<OffenceFact> offence,
        List<DefenceFact> defence,
        List<BuffFact> buffs,
        List<MobilityFact> mobility,
        List<PassiveFact> passives,
        List<String> passiveTags,
        List<ItemFact> items) {
}
