package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record OperationFlow(String subject, List<String> steps, List<OperationFact> grounds) {
}
