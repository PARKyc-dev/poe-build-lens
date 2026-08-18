package com.parkyc.poelens.build.domain.dto;

public record SupportGemFact(
        String name,
        Integer level,
        Integer quality,
        String qualityType,
        Boolean enabled,
        Boolean awakened) {
}
