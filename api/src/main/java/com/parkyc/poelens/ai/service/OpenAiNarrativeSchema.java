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
        List<String> defenceSources = facts == null ? List.of() : NarrativeSectionValidator.defenceEvidenceNames(facts).stream().sorted().toList();
        List<String> defenceKinds = NarrativeSectionValidator.defenceSectionSubjects(facts);
        List<String> buffNames = facts == null || facts.buffs() == null ? List.of()
                : facts.buffs().stream().filter(value -> value.tags() != null && !value.tags().isEmpty())
                .map(value -> value.name()).distinct().sorted().toList();
        List<String> buffSources = facts == null ? List.of() : NarrativeSectionValidator.buffEvidenceNames(facts).stream().sorted().toList();
        Map<String, Object> offenceSections = attacks.isEmpty()
                ? Map.of("type", "array", "maxItems", 0, "items", offenceSection(null, sources, List.of()))
                : Map.of("type", "array", "items", Map.of("anyOf", attacks.stream().map(attack -> offenceSection(attack, sources,
                        flows == null ? List.of() : flows.stream()
                                .filter(flow -> attack.equals(flow.subject()) || (flow.grounds() != null && flow.grounds().stream()
                                        .anyMatch(ground -> attack.equals(ground.sourceName()))))
                                .map(OperationFlow::subject).distinct().sorted().toList())).toList()));
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "buildSummary", Map.of("type", "string", "description",
                                "숫자와 세부 출처 나열 없이 핵심 공격·방어·버프 상호작용만 설명하는 3~5문장 요약"),
                        "offenceSections", offenceSections,
                        "defenceSections", structuredSectionSchema("defenceKind", defenceKinds, List.of("resource", "mitigation", "avoidance", "recovery"), defenceSources, true),
                        "buffSections", structuredSectionSchema("buffName", buffNames, List.of("offence", "defence", "utility"), buffSources, false)),
                "required", List.of("buildSummary", "offenceSections", "defenceSections", "buffSections"),
                "additionalProperties", false);
    }

    private Map<String, Object> offenceSection(String attack, List<String> sources, List<String> subjects) {
        return Map.of("type", "object", "properties", Map.of(
                "attackName", namesSchema(attack == null ? List.of() : List.of(attack)),
                "section", namesSchema(List.of("core", "supports", "modifiers", "operation")),
                "explanation", Map.of("type", "string"),
                "details", Map.of("type", "array", "minItems", 1, "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "label", Map.of("type", "string"),
                                "explanation", Map.of("type", "string"),
                                "type", namesSchema(List.of("step", "interaction", "condition"))),
                        "required", List.of("label", "explanation", "type"),
                        "additionalProperties", false)),
                "evidence", Map.of("type", "array", "items", namesSchema(sources)),
                "flowSubjects", subjects.isEmpty()
                        ? Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 0)
                        : Map.of("type", "array", "items", namesSchema(subjects))),
                "required", List.of("attackName", "section", "explanation", "details", "evidence", "flowSubjects"),
                "additionalProperties", false);
    }

    private Map<String, Object> namesSchema(List<String> names) {
        return names.isEmpty() ? Map.of("type", "string") : Map.of("type", "string", "enum", names);
    }

    private Map<String, Object> structuredSectionSchema(String subject, List<String> subjects, List<String> sectionKinds,
                                                        List<String> evidenceNames, boolean includeDetails) {
        Map<String, Object> properties = new java.util.HashMap<>(Map.of(
                subject, namesSchema(subjects), "section", Map.of("type", "string", "enum", sectionKinds),
                "explanation", Map.of("type", "string"), "evidence", Map.of("type", "array", "items", namesSchema(evidenceNames))));
        List<String> required = new java.util.ArrayList<>(List.of(subject, "section", "explanation", "evidence"));
        if (includeDetails) {
            properties.put("details", detailArraySchema());
            required.add("details");
        }
        Map<String, Object> item = Map.of("type", "object", "properties", properties,
                "required", required, "additionalProperties", false);
        return subjects.isEmpty()
                ? Map.of("type", "array", "maxItems", 0, "items", item)
                : Map.of("type", "array", "items", item);
    }

    private Map<String, Object> detailArraySchema() {
        return Map.of("type", "array", "minItems", 1, "items", Map.of(
                "type", "object",
                "properties", Map.of(
                        "label", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "type", namesSchema(List.of("step", "interaction", "condition"))),
                "required", List.of("label", "explanation", "type"),
                "additionalProperties", false));
    }
}
