package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record SkillFact(
        String name,
        Integer level,
        Integer quality,
        String qualityType,
        Boolean enabled,
        Boolean awakened,
        List<SupportGemFact> supports,
        List<String> effects) {
    public SkillFact(String name, Integer level, Integer quality, String qualityType,
                     Boolean enabled, Boolean awakened, List<SupportGemFact> supports) {
        this(name, level, quality, qualityType, enabled, awakened, supports, List.of());
    }
}
