package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiNarrativeSchema {
    public Map<String, Object> create() {
        return create(null, List.of());
    }

    public Map<String, Object> create(BuildFacts facts, List<OperationFlow> flows) {
        List<String> attacks = facts == null || facts.offence() == null ? List.of()
                : facts.offence().stream().map(attack -> attack.name()).distinct().toList();
        List<String> sources = facts == null ? List.of() : NarrativeSectionValidator.sourceNames(facts).stream().sorted().toList();
        Map<String, Object> offenceItem = attacks.isEmpty() ? offenceSection(null, sources, List.of())
                : Map.of("anyOf", attacks.stream().map(attack -> offenceSection(attack, sources,
                        flows == null ? List.of() : flows.stream()
                                .filter(flow -> attack.equals(flow.subject()) || (flow.grounds() != null && flow.grounds().stream()
                                        .anyMatch(ground -> attack.equals(ground.sourceName()))))
                                .map(OperationFlow::subject).distinct().sorted().toList())).toList());
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "buildSummary", Map.of("type", "string"),
                        "offenceSections", Map.of("type", "array", "items", offenceItem),
                        "defenceSections", structuredSectionSchema("defenceKind", List.of("resource", "mitigation", "avoidance", "recovery")),
                        "buffSections", structuredSectionSchema("buffName", List.of("offence", "defence", "utility"))),
                "required", List.of("buildSummary", "offenceSections", "defenceSections", "buffSections"),
                "additionalProperties", false);
    }

    private Map<String, Object> offenceSection(String attack, List<String> sources, List<String> subjects) {
        return Map.of("type", "object", "properties", Map.of(
                "attackName", namesSchema(attack == null ? List.of() : List.of(attack)),
                "section", namesSchema(List.of("core", "supports", "modifiers", "operation")),
                "explanation", Map.of("type", "string"),
                "evidence", Map.of("type", "array", "items", namesSchema(sources)),
                "flowSubjects", subjects.isEmpty()
                        ? Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 0)
                        : Map.of("type", "array", "items", namesSchema(subjects))),
                "required", List.of("attackName", "section", "explanation", "evidence", "flowSubjects"),
                "additionalProperties", false);
    }

    private Map<String, Object> namesSchema(List<String> names) {
        return names.isEmpty() ? Map.of("type", "string") : Map.of("type", "string", "enum", names);
    }

    private Map<String, Object> structuredSectionSchema(String subject, List<String> sectionKinds) {
        return Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of(
                subject, Map.of("type", "string"), "section", Map.of("type", "string", "enum", sectionKinds),
                "explanation", Map.of("type", "string"), "evidence", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of(subject, "section", "explanation", "evidence"), "additionalProperties", false));
    }
}
