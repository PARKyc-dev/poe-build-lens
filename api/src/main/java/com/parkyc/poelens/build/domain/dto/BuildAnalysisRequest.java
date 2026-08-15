package com.parkyc.poelens.build.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildAnalysisRequest(
        @NotBlank(message = "Game version is required.") String gameVersion,
        BuildFacts buildFacts) {
}
