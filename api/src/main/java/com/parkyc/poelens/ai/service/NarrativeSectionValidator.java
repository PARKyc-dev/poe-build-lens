package com.parkyc.poelens.ai.service;

import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Component
public class NarrativeSectionValidator {
    public NarrativeResult validate(Map<String, Object> narrative, BuildFacts facts, NarrativeResult fallback) {
        return new NarrativeResult(
                stringOrFallback(narrative.get("buildSummary"), fallback.summary()),
                offenceSections(narrative.get("offenceSections"), facts, fallback.offence()),
                defenceSections(narrative.get("defenceSections"), facts, fallback.defence()),
                buffSections(narrative.get("buffSections"), facts, fallback.buffs()));
    }

    private List<Mechanic> offenceSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        Map<String, Set<String>> evidence = new HashMap<>();
        for (OffenceFact attack : facts.offence() == null ? List.<OffenceFact>of() : facts.offence()) {
            Set<String> values = new HashSet<>();
            values.add(attack.name());
            if (attack.modifiers() != null) {
                for (var modifier : attack.modifiers()) {
                    values.add(modifier.name());
                    values.add(modifier.source());
                }
            }
            if (facts.skills() != null) {
                facts.skills().stream().filter(skill -> attack.name().equals(skill.name())).findFirst()
                        .ifPresent(skill -> {
                            if (skill.supports() != null) skill.supports().forEach(support -> values.add(support.name()));
                        });
            }
            evidence.put(attack.name(), values);
        }
        if (!(value instanceof List<?> sections) || sections.isEmpty()) return fallback;

        List<Mechanic> result = new ArrayList<>();
        Set<String> coveredAttacks = new HashSet<>();
        for (Object sectionValue : sections) {
            if (!(sectionValue instanceof Map<?, ?> section)) return fallback;
            String attackName = string(section.get("attackName"));
            String kind = string(section.get("section"));
            String explanation = string(section.get("explanation"));
            if (attackName == null || kind == null || !Set.of("core", "supports", "modifiers", "operation").contains(kind)
                    || explanation == null || !(section.get("evidence") instanceof List<?> references) || references.isEmpty()
                    || !evidence.containsKey(attackName)) return fallback;
            for (Object reference : references) {
                if (!(reference instanceof String name) || !evidence.get(attackName).contains(name)) return fallback;
            }
            result.add(new Mechanic(offenceSectionTitle(kind, attackName), explanation));
            coveredAttacks.add(attackName);
        }
        return result.isEmpty() || !coveredAttacks.containsAll(evidence.keySet()) ? fallback : result;
    }

    private List<Mechanic> defenceSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        Set<String> evidence = new HashSet<>();
        if (facts.defence() != null) facts.defence().forEach(fact -> evidence.add(fact.kind()));
        return structuredSections(value, "defenceKind", Set.of("resource", "mitigation", "avoidance", "recovery"), evidence, fallback,
                this::defenceSectionTitle);
    }

    private List<Mechanic> buffSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        Map<String, Set<String>> evidence = new HashMap<>();
        if (facts.buffs() != null) facts.buffs().forEach(buff -> {
            if (buff.tags() == null || buff.tags().isEmpty()) return;
            Set<String> values = new HashSet<>();
            values.add(buff.name());
            values.addAll(buff.tags());
            evidence.put(buff.name(), values);
        });
        return structuredSections(value, "buffName", Set.of("offence", "defence", "utility"), evidence, fallback, this::buffSectionTitle);
    }

    private List<Mechanic> structuredSections(Object value, String subjectKey, Set<String> kinds, Set<String> evidence,
                                               List<Mechanic> fallback, BiFunction<String, String, String> title) {
        Map<String, Set<String>> subjects = new HashMap<>();
        evidence.forEach(subject -> subjects.put(subject, Set.of(subject)));
        return structuredSections(value, subjectKey, kinds, subjects, fallback, title);
    }

    private List<Mechanic> structuredSections(Object value, String subjectKey, Set<String> kinds, Map<String, Set<String>> evidence,
                                               List<Mechanic> fallback, BiFunction<String, String, String> title) {
        if (!(value instanceof List<?> sections) || sections.isEmpty()) return fallback;
        List<Mechanic> result = new ArrayList<>();
        Set<String> coveredSubjects = new HashSet<>();
        for (Object sectionValue : sections) {
            if (!(sectionValue instanceof Map<?, ?> section)) return fallback;
            String subject = string(section.get(subjectKey));
            String kind = string(section.get("section"));
            String explanation = string(section.get("explanation"));
            if (subject == null || kind == null || !kinds.contains(kind) || explanation == null
                    || !(section.get("evidence") instanceof List<?> references) || references.isEmpty() || !evidence.containsKey(subject)) return fallback;
            boolean containsSubject = false;
            for (Object reference : references) {
                if (!(reference instanceof String name) || !evidence.get(subject).contains(name)) return fallback;
                containsSubject |= subject.equals(name);
            }
            if (!containsSubject) return fallback;
            result.add(new Mechanic(title.apply(kind, subject), explanation));
            coveredSubjects.add(subject);
        }
        return result.isEmpty() || !coveredSubjects.containsAll(evidence.keySet()) ? fallback : result;
    }

    private String offenceSectionTitle(String kind, String attackName) {
        return switch (kind) {
            case "core" -> "핵심 동작: " + attackName;
            case "supports" -> "보조젬 연결: " + attackName;
            case "modifiers" -> "적용된 빌드 효과: " + attackName;
            case "operation" -> "운용 방식: " + attackName;
            default -> throw new IllegalArgumentException("알 수 없는 공격 섹션입니다: " + kind);
        };
    }

    private String defenceSectionTitle(String kind, String defenceKind) {
        return switch (kind) {
            case "resource" -> "방어 자원: " + defenceKind;
            case "mitigation" -> "피해 경감: " + defenceKind;
            case "avoidance" -> "회피·막기: " + defenceKind;
            case "recovery" -> "회복: " + defenceKind;
            default -> throw new IllegalArgumentException("알 수 없는 방어 섹션입니다: " + kind);
        };
    }

    private String buffSectionTitle(String kind, String buffName) {
        return switch (kind) {
            case "offence" -> "공격 버프: " + buffName;
            case "defence" -> "방어 버프: " + buffName;
            case "utility" -> "유틸리티 버프: " + buffName;
            default -> throw new IllegalArgumentException("알 수 없는 버프 섹션입니다: " + kind);
        };
    }

    private String stringOrFallback(Object value, String fallback) {
        String result = string(value);
        return result == null ? fallback : result;
    }

    private String string(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
