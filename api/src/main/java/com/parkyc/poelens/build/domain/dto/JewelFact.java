package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record JewelFact(
        String socket,
        String name,
        String baseName,
        String rarity,
        List<String> modifiers,
        String kind,
        List<String> tags) {
}
