package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record AnalysisResult(
        String gameVersion,
        List<Mechanic> offence,
        List<Mechanic> defence,
        List<Mechanic> buffs,
        List<Mechanic> mobility,
        List<Mechanic> passives,
        List<Mechanic> passiveNodes,
        List<Mechanic> ascendancies,
        List<Mechanic> gear,
        List<Mechanic> performance,
        List<Mechanic> overrides,
        List<String> unverified,
        List<Evidence> evidence) {
}
