package com.parkyc.poelens.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAiNarrativeClient implements NarrativeRefiner {
    private static final Logger log = LogManager.getLogger(OpenAiNarrativeClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final HttpClient client = HttpClient.newHttpClient();
    private final PromptLogWriter promptLogWriter;
    private final OpenAiResponseParser responseParser;
    private final BuildNarrativePromptBuilder promptBuilder;

    @Value("${poe-lens.openai.enabled:false}")
    private boolean enabled;

    @Value("${poe-lens.openai.api-key:}")
    private String apiKey;

    @Value("${poe-lens.openai.model}")
    private String model;

    public OpenAiNarrativeClient(PromptLogWriter promptLogWriter, OpenAiResponseParser responseParser,
                                 BuildNarrativePromptBuilder promptBuilder) {
        this.promptLogWriter = promptLogWriter;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public NarrativeResult refine(BuildFacts facts, String summary, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs) {
        if (!enabled || apiKey.isBlank()) {
            log.info("OpenAI 기재 보정 건너뜀: 사용 설정={}, API 키 설정={}", enabled, !apiKey.isBlank());
            return new NarrativeResult(summary, offence, defence, buffs);
        }

        try {
            return queue.submit(() -> generate(facts, summary, offence, defence, buffs)).get(45, TimeUnit.SECONDS);
        } catch (Exception exception) {
            log.warn("OpenAI 기재 보정에 실패해 규칙 기반 기재를 사용합니다", exception);
            return new NarrativeResult(summary, offence, defence, buffs);
        }
    }

    private NarrativeResult generate(BuildFacts facts, String fallbackSummary,
                                     List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs) throws Exception {
        String prompt = promptBuilder.build(facts);
        long startedAt = System.nanoTime();
        log.info("OpenAI 기재 요청: 모델={}, 프롬프트 길이={}", model, prompt.length());

        Map<String, Object> schema = Map.of(
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
        String body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", prompt,
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "build_narrative",
                        "strict", true,
                        "schema", schema))));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        log.info("OpenAI 기재 응답 수신: 모델={}, 상태={}, 처리 시간(ms)={}", model, response.statusCode(), elapsedMillis(startedAt));
        Map<String, Object> narrative;
        try {
            narrative = responseParser.parse(responseBody);
            promptLogWriter.write(prompt, responseParser.formatForLog(narrative));
        } catch (Exception exception) {
            promptLogWriter.write(prompt, responseBody);
            throw exception;
        }

        return new NarrativeResult(
                java.util.Objects.requireNonNullElse(string(narrative.get("buildSummary")), fallbackSummary),
                offenceSections(narrative.get("offenceSections"), facts, offence),
                defenceSections(narrative.get("defenceSections"), facts, defence),
                buffSections(narrative.get("buffSections"), facts, buffs));
    }

    private Map<String, Object> structuredSectionSchema(String subject, List<String> sectionKinds) {
        return Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of(
                subject, Map.of("type", "string"), "section", Map.of("type", "string", "enum", sectionKinds),
                "explanation", Map.of("type", "string"), "evidence", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of(subject, "section", "explanation", "evidence"), "additionalProperties", false));
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    @SuppressWarnings("unchecked")
    static List<Mechanic> offenceSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        if (!(value instanceof List<?> sections) || sections.isEmpty()) return fallback;
        Map<String, Set<String>> evidence = new java.util.HashMap<>();
        for (var attack : facts.offence() == null ? List.<com.parkyc.poelens.build.domain.dto.OffenceFact>of() : facts.offence()) {
            Set<String> values = new java.util.HashSet<>();
            values.add(attack.name());
            if (attack.modifiers() != null) for (var modifier : attack.modifiers()) { values.add(modifier.name()); values.add(modifier.source()); }
            if (facts.skills() != null) facts.skills().stream().filter(skill -> attack.name().equals(skill.name())).findFirst()
                    .ifPresent(skill -> { if (skill.supports() != null) skill.supports().forEach(support -> values.add(support.name())); });
            evidence.put(attack.name(), values);
        }
        List<Mechanic> result = new java.util.ArrayList<>();
        Set<String> coveredAttacks = new java.util.HashSet<>();
        for (Object valueSection : sections) {
            if (!(valueSection instanceof Map<?, ?> section)) return fallback;
            String attackName = string(section.get("attackName"));
            String kind = string(section.get("section"));
            String explanation = string(section.get("explanation"));
            if (attackName == null || kind == null || !Set.of("core", "supports", "modifiers", "operation").contains(kind) || explanation == null || !(section.get("evidence") instanceof List<?> references) || references.isEmpty() || !evidence.containsKey(attackName)) return fallback;
            for (Object reference : references) if (!(reference instanceof String name) || !evidence.get(attackName).contains(name)) return fallback;
            result.add(new Mechanic(sectionTitle(kind, attackName), explanation));
            coveredAttacks.add(attackName);
        }
        return result.isEmpty() || !coveredAttacks.containsAll(evidence.keySet()) ? fallback : result;
    }

    static List<Mechanic> defenceSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        Set<String> evidence = new java.util.HashSet<>();
        if (facts.defence() != null) facts.defence().forEach(fact -> evidence.add(fact.kind()));
        return structuredSections(value, "defenceKind", Set.of("resource", "mitigation", "avoidance", "recovery"), evidence, fallback,
                (kind, subject) -> defenceSectionTitle(kind, subject));
    }

    static List<Mechanic> buffSections(Object value, BuildFacts facts, List<Mechanic> fallback) {
        Map<String, Set<String>> evidence = new java.util.HashMap<>();
        if (facts.buffs() != null) facts.buffs().forEach(buff -> {
            Set<String> values = new java.util.HashSet<>();
            values.add(buff.name());
            if (buff.tags() != null) values.addAll(buff.tags());
            evidence.put(buff.name(), values);
        });
        return structuredSections(value, "buffName", Set.of("offence", "defence", "utility"), evidence, fallback,
                (kind, subject) -> buffSectionTitle(kind, subject));
    }

    @SuppressWarnings("unchecked")
    private static List<Mechanic> structuredSections(Object value, String subjectKey, Set<String> kinds, Set<String> evidence, List<Mechanic> fallback,
                                                      java.util.function.BiFunction<String, String, String> title) {
        Map<String, Set<String>> subjects = new java.util.HashMap<>();
        evidence.forEach(subject -> subjects.put(subject, Set.of(subject)));
        return structuredSections(value, subjectKey, kinds, subjects, fallback, title);
    }

    @SuppressWarnings("unchecked")
    private static List<Mechanic> structuredSections(Object value, String subjectKey, Set<String> kinds, Map<String, Set<String>> evidence, List<Mechanic> fallback,
                                                      java.util.function.BiFunction<String, String, String> title) {
        if (!(value instanceof List<?> sections) || sections.isEmpty()) return fallback;
        List<Mechanic> result = new java.util.ArrayList<>();
        Set<String> coveredSubjects = new java.util.HashSet<>();
        for (Object sectionValue : sections) {
            if (!(sectionValue instanceof Map<?, ?> section)) return fallback;
            String subject = string(section.get(subjectKey));
            String kind = string(section.get("section"));
            String explanation = string(section.get("explanation"));
            if (subject == null || kind == null || !kinds.contains(kind) || explanation == null || !(section.get("evidence") instanceof List<?> references) || references.isEmpty() || !evidence.containsKey(subject)) return fallback;
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

    private static String sectionTitle(String kind, String attackName) {
        return switch (kind) {
            case "core" -> "핵심 동작: " + attackName;
            case "supports" -> "보조젬 연결: " + attackName;
            case "modifiers" -> "적용된 빌드 효과: " + attackName;
            case "operation" -> "운용 방식: " + attackName;
            default -> throw new IllegalArgumentException("알 수 없는 공격 섹션입니다: " + kind);
        };
    }

    private static String defenceSectionTitle(String kind, String defenceKind) {
        return switch (kind) {
            case "resource" -> "방어 자원: " + defenceKind;
            case "mitigation" -> "피해 경감: " + defenceKind;
            case "avoidance" -> "회피·막기: " + defenceKind;
            case "recovery" -> "회복: " + defenceKind;
            default -> throw new IllegalArgumentException("알 수 없는 방어 섹션입니다: " + kind);
        };
    }

    private static String buffSectionTitle(String kind, String buffName) {
        return switch (kind) {
            case "offence" -> "공격 버프: " + buffName;
            case "defence" -> "방어 버프: " + buffName;
            case "utility" -> "유틸리티 버프: " + buffName;
            default -> throw new IllegalArgumentException("알 수 없는 버프 섹션입니다: " + kind);
        };
    }

    private static String string(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
