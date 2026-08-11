package com.parkyc.poelens.analysis.model;

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
