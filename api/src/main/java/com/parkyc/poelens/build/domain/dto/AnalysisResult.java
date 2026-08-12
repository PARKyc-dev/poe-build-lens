package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record AnalysisResult(
        String gameVersion,
        String overview,
        List<Mechanic> interactions,
        List<String> contributors,
        List<String> items,
        List<String> defences,
        List<String> resourceSustain,
        List<String> unverified,
        List<Evidence> evidence) {
}
