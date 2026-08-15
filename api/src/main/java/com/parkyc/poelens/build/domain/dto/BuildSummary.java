package com.parkyc.poelens.build.domain.dto;

public record BuildSummary(
        Double totalDps,
        Double combinedDps,
        Double life,
        Double energyShield,
        Double mana,
        Double armour,
        Double evasion,
        Double totalEhp) {
}
