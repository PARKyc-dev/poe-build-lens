package com.parkyc.poelens.build.domain.dto;

public record PerformanceFact(
        Double totalDps,
        Double combinedDps,
        Double life,
        Double energyShield,
        Double mana,
        Double armour,
        Double evasion,
        Double totalEhp) {
}
