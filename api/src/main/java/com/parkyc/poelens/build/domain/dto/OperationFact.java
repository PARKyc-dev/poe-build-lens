package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record OperationFact(String sourceType, String sourceName, String action, String subject, List<String> effects) {
}
