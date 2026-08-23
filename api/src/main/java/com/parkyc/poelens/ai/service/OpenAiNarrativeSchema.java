package com.parkyc.poelens.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiNarrativeSchema {
    public Map<String, Object> create() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "buildSummary", Map.of("type", "string"),
                        "offenceSections", Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of(
                                "attackName", Map.of("type", "string"), "section", Map.of("type", "string", "enum", List.of("core", "supports", "modifiers", "operation")),
                                "explanation", Map.of("type", "string"), "evidence", Map.of("type", "array", "items", Map.of("type", "string"))),
                                "required", List.of("attackName", "section", "explanation", "evidence"), "additionalProperties", false)),
                        "defenceSections", structuredSectionSchema("defenceKind", List.of("resource", "mitigation", "avoidance", "recovery")),
                        "buffSections", structuredSectionSchema("buffName", List.of("offence", "defence", "utility"))),
                "required", List.of("buildSummary", "offenceSections", "defenceSections", "buffSections"),
                "additionalProperties", false);
    }

    private Map<String, Object> structuredSectionSchema(String subject, List<String> sectionKinds) {
        return Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of(
                subject, Map.of("type", "string"), "section", Map.of("type", "string", "enum", sectionKinds),
                "explanation", Map.of("type", "string"), "evidence", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of(subject, "section", "explanation", "evidence"), "additionalProperties", false));
    }
}
