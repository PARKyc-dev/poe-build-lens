package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record NarrativeResult(
        String summary,
        List<Mechanic> offence,
        List<Mechanic> defence,
        List<Mechanic> buffs) {
}
