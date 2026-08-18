package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record OffenceFact(String name, String role, Double combinedDps, String delivery, List<String> tags) {
}
