package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.OperationFact;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OperationFlowAnalyzer {

    public List<OperationFlow> analyse(List<OffenceFact> offence, List<OperationFact> facts) {
        List<OperationFact> operationFacts = safe(facts);
        List<OperationFlow> flows = new ArrayList<>();
        addResourceCycles(operationFacts, flows);
        addConditionFlows(offence, operationFacts, flows);
        addIndependentFlows(operationFacts, flows);
        return flows;
    }

    private void addResourceCycles(List<OperationFact> facts, List<OperationFlow> flows) {
        Map<String, List<OperationFact>> consumed = bySubject(facts, "consume");
        Map<String, List<OperationFact>> gained = bySubject(facts, "gain");
        for (Map.Entry<String, List<OperationFact>> entry : consumed.entrySet()) {
            List<OperationFact> gains = gained.get(entry.getKey());
            if (gains == null || gains.isEmpty()) continue;
            List<OperationFact> grounds = new ArrayList<>(entry.getValue());
            grounds.addAll(gains);
            String subject = entry.getKey();
            flows.add(new OperationFlow(subject, List.of(subject + " 소비", subject + " 획득", "연속 사용"), grounds));
        }
    }

    private void addConditionFlows(List<OffenceFact> offence, List<OperationFact> facts, List<OperationFlow> flows) {
        List<OffenceFact> primaryOffence = safe(offence).stream()
                .filter(fact -> "primary".equals(fact.role()))
                .filter(fact -> hasText(fact.name()))
                .toList();
        if (primaryOffence.isEmpty()) return;

        for (OperationFact fact : facts) {
            List<String> steps = conditionSteps(fact.action());
            if (steps == null) continue;
            List<OperationFact> grounds = new ArrayList<>();
            grounds.add(fact);
            grounds.addAll(triggerFactsFor(fact, facts));
            for (OffenceFact primary : primaryOffence) {
                flows.add(new OperationFlow(primary.name(), steps, grounds));
            }
        }
    }

    private void addIndependentFlows(List<OperationFact> facts, List<OperationFlow> flows) {
        for (OperationFact fact : facts) {
            if (!hasText(fact.subject())) continue;
            String step = independentStep(fact);
            if (step != null) flows.add(new OperationFlow(fact.subject(), List.of(step), List.of(fact)));
        }
    }

    private Map<String, List<OperationFact>> bySubject(List<OperationFact> facts, String action) {
        Map<String, List<OperationFact>> grouped = new LinkedHashMap<>();
        for (OperationFact fact : facts) {
            if (action.equals(fact.action()) && hasText(fact.subject())) {
                grouped.computeIfAbsent(fact.subject(), ignored -> new ArrayList<>()).add(fact);
            }
        }
        return grouped;
    }

    private List<OperationFact> triggerFactsFor(OperationFact condition, List<OperationFact> facts) {
        return facts.stream()
                .filter(fact -> "trigger".equals(fact.action()))
                .filter(fact -> java.util.Objects.equals(condition.subject(), fact.subject()))
                .toList();
    }

    private List<String> conditionSteps(String action) {
        return switch (action) {
            case "on-kill" -> List.of("피해", "처치", "후속 효과");
            case "on-hit" -> List.of("피해", "명중", "후속 효과");
            case "on-damaged" -> List.of("피해", "피격", "후속 효과");
            default -> null;
        };
    }

    private String independentStep(OperationFact fact) {
        return switch (fact.action()) {
            case "maintain" -> fact.subject() + " 유지";
            case "reserve" -> fact.subject() + " 예약";
            case "cooldown" -> fact.subject() + " 재사용 대기시간";
            case "convert" -> fact.subject() + " 전환";
            case "enhance" -> fact.subject() + " 강화";
            default -> null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
