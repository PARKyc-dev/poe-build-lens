package com.parkyc.poelens.build.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildAnalysisRequest(@NotBlank(message = "PoB build input is required.") String pobInput) {
}
