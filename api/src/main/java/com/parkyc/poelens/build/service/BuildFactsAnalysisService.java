package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.domain.dto.BuffFact;
import com.parkyc.poelens.build.domain.dto.DefenceFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.PassiveFact;
import com.parkyc.poelens.build.domain.dto.MobilityFact;
import com.parkyc.poelens.build.domain.dto.ItemFact;
import com.parkyc.poelens.build.domain.dto.SkillFact;
import com.parkyc.poelens.build.domain.dto.SupportGemFact;
import com.parkyc.poelens.build.domain.dto.AscendancyFact;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BuildFactsAnalysisService {

    private static final Map<String, String> DELIVERY_TITLES = Map.of(
            "self-cast", "직접 시전 (Self-Cast)",
            "attack", "공격 (Attack)",
            "totem", "토템 (Totem)",
            "trap", "트랩 (Trap)",
            "mine", "마인 (Mine)",
            "minion", "소환수 (Minion)",
            "brand", "브랜드 (Brand)",
            "trigger", "트리거 (Trigger)");
    private static final Map<String, String> BUFF_KIND_TITLES = Map.of(
            "aura", "오라 (Aura)",
            "curse", "저주 (Curse)",
            "guard", "가드 (Guard)",
            "flask", "플라스크 (Flask)",
            "buff", "버프 (Buff)");

    public List<Mechanic> analyseOffence(List<OffenceFact> facts, List<ItemFact> items, List<SkillFact> skills) {
        List<Mechanic> analysis = new ArrayList<>();
        Set<String> itemTags = new HashSet<>();
        for (ItemFact item : safe(items)) itemTags.addAll(safe(item.tags()));
        for (OffenceFact fact : safe(facts)) {
            Set<String> skillTags = new HashSet<>(safe(fact.tags()));
            String itemSupport = hasAny(itemTags, skillTags.toArray(String[]::new)) ? " 장비 효과 태그가 이 피해 축을 보강합니다." : "";
            String gemSupport = supportSummary(fact.name(), skills);
            if ("primary".equals(fact.role())) {
                analysis.add(new Mechanic("주력 공격: " + fact.name(),
                        deliveryTitle(fact.delivery()) + " 방식으로 " + skillProfile(fact.tags()) + " 태그를 활용해 주 피해를 담당합니다." + itemSupport + gemSupport));
            } else if ("secondary".equals(fact.role())) {
                analysis.add(new Mechanic("보조 공격: " + fact.name(),
                        deliveryTitle(fact.delivery()) + " 방식으로 " + skillProfile(fact.tags()) + " 태그를 활용해 주력 공격을 보완합니다." + itemSupport + gemSupport));
            }
        }
        return analysis;
    }

    public List<Mechanic> analyseDefence(List<DefenceFact> facts, List<PassiveFact> passives, List<String> passiveTags, List<ItemFact> items) {
        List<Mechanic> analysis = new ArrayList<>();
        Set<String> kinds = safe(facts).stream()
                .filter(fact -> fact.value() != null && fact.value() > 0)
                .map(DefenceFact::kind)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> supportTags = supportTags(passives, passiveTags, items);
        if (hasAny(kinds, "life", "energy-shield")) {
            analysis.add(new Mechanic("생존 자원 기반 방어",
                    resourceProfile(kinds) + "을 피해를 견디는 기본 자원으로 사용합니다." + supportSuffix(supportTags, "life", "energy-shield", "life-regeneration", "energy-shield-recovery")));
        }
        if (kinds.containsAll(Set.of("fire-resistance", "cold-resistance", "lightning-resistance"))) {
            analysis.add(new Mechanic("원소 저항 기반 방어",
                    "화염·냉기·번개 저항 수치로 원소 피해를 줄입니다." + supportSuffix(supportTags, "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance")));
        }
        if (hasAny(kinds, "armour", "evasion", "physical-mitigation")) {
            analysis.add(new Mechanic("방어도·회피 기반 방어",
                    defenceProfile(kinds, "armour", "방어도", "evasion", "회피", "physical-mitigation", "물리 피해 감소") + "로 물리 적중 피해를 줄이거나 피합니다." + supportSuffix(supportTags, "armour", "evasion", "physical-mitigation")));
        }
        if (hasAny(kinds, "block", "spell-block")) {
            analysis.add(new Mechanic("막기 기반 방어",
                    defenceProfile(kinds, "block", "공격 막기", "spell-block", "주문 막기") + "로 적중 피해를 막습니다." + supportSuffix(supportTags, "block", "spell-block")));
        }
        if (kinds.contains("spell-suppression")) {
            analysis.add(new Mechanic("주문 방어",
                    "주문 억제 수치로 주문 적중 피해를 줄입니다." + supportSuffix(supportTags, "spell-suppression")));
        }
        if (hasAny(kinds, "ward", "guard", "attack-dodge", "spell-dodge", "damage-avoidance")) {
            analysis.add(new Mechanic("보조 피해 방어",
                    defenceProfile(kinds, "ward", "와드", "guard", "가드", "attack-dodge", "공격 회피", "spell-dodge", "주문 회피", "damage-avoidance", "피해 회피") + "로 추가 피해 방어 층을 구성합니다." + supportSuffix(supportTags, "ward", "guard", "attack-dodge", "spell-dodge", "damage-avoidance")));
        }
        return analysis;
    }

    public List<Mechanic> analysePassives(List<PassiveFact> passives, List<String> passiveTags) {
        Set<String> tags = new HashSet<>(safe(passiveTags));
        for (PassiveFact passive : safe(passives)) tags.addAll(safe(passive.tags()));
        List<Mechanic> analysis = new ArrayList<>();
        if (hasAny(tags, "damage-over-time", "fire", "cold", "lightning", "chaos", "physical", "attack", "spell", "minion")) {
            analysis.add(new Mechanic("피해 핵심 패시브", "패시브 효과 태그가 " + damageProfile(tags) + " 피해 축을 보강합니다."));
        }
        if (hasAny(tags, "life", "energy-shield", "life-regeneration", "energy-shield-recovery", "armour", "evasion", "ward", "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance")) {
            analysis.add(new Mechanic("생존 핵심 패시브", "패시브 효과 태그가 " + survivalProfile(tags) + "을 보강합니다."));
        }
        if (hasAny(tags, "block", "spell-block", "spell-suppression", "attack-dodge", "spell-dodge", "damage-avoidance", "guard")) {
            analysis.add(new Mechanic("적중 방어 핵심 패시브", "패시브 효과 태그가 " + hitDefenceProfile(tags) + "을 보강합니다."));
        }
        return analysis;
    }

    public List<Mechanic> analysePassiveNodes(List<PassiveFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (PassiveFact fact : safe(facts)) {
            if (fact.name() != null && !fact.name().isBlank()) {
                String effect = safe(fact.effects()).isEmpty() ? "효과 정보가 없습니다." : String.join(" · ", fact.effects());
                analysis.add(new Mechanic("주요 패시브: " + fact.name(), "적용된 효과: " + effect));
            }
        }
        return analysis;
    }

    public List<Mechanic> analyseAscendancies(List<AscendancyFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (AscendancyFact fact : safe(facts)) {
            if (fact.name() != null && !fact.name().isBlank()) {
                String effect = safe(fact.effects()).isEmpty() ? "효과 정보가 없습니다." : String.join(" · ", fact.effects());
                analysis.add(new Mechanic("전직 노드: " + fact.name(),
                        (fact.ascendancyName() == null ? "전직" : fact.ascendancyName() + " 전직") + "의 적용된 효과: " + effect));
            }
        }
        return analysis;
    }

    public List<Mechanic> analyseBuffs(List<BuffFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (BuffFact fact : safe(facts)) {
            Set<String> tags = new HashSet<>(safe(fact.tags()));
            String kind = BUFF_KIND_TITLES.getOrDefault(fact.kind(), "유틸리티");
            String title = tags.isEmpty() ? kind + ": " + fact.name() : "버프 유틸리티: " + fact.name();
            if (tags.isEmpty()) {
                analysis.add(new Mechanic(title, "활성화된 효과가 플레이어에게 적용되어 있습니다."));
            } else {
                analysis.add(new Mechanic(title, utilityProfile(tags) + " 태그가 활성화되어 " + utilityEffect(tags) + "를 보강합니다."));
            }
        }
        return analysis;
    }

    private Set<String> supportTags(List<PassiveFact> passives, List<String> passiveTags, List<ItemFact> items) {
        Set<String> supportTags = new HashSet<>(safe(passiveTags));
        for (PassiveFact passive : safe(passives)) supportTags.addAll(safe(passive.tags()));
        for (ItemFact item : safe(items)) supportTags.addAll(safe(item.tags()));
        return supportTags;
    }

    public List<Mechanic> analyseMobility(List<MobilityFact> facts) {
        List<Mechanic> analysis = new ArrayList<>();
        for (MobilityFact fact : safe(facts)) {
            if (fact.name() != null && !fact.name().isBlank()) {
                analysis.add(new Mechanic("이동기: " + fact.name(),
                        "피해 분석에서 제외하고 위치 이동과 전투 진입·이탈에 사용하는 이동기입니다."));
            }
        }
        return analysis;
    }

    private String deliveryTitle(String delivery) {
        return DELIVERY_TITLES.getOrDefault(delivery, "확인 불가 (Unverified)");
    }

    private String supportSummary(String skillName, List<SkillFact> skills) {
        for (SkillFact skill : safe(skills)) {
            if (skillName != null && skillName.equals(skill.name()) && !safe(skill.supports()).isEmpty()) {
                return " 연결된 보조 젬: " + safe(skill.supports()).stream()
                        .map(this::supportDescription)
                        .collect(java.util.stream.Collectors.joining(", ")) + ".";
            }
        }
        return "";
    }

    private String supportDescription(SupportGemFact support) {
        String level = support.level() == null ? "?" : support.level().toString();
        String quality = support.quality() == null ? "?" : support.quality().toString();
        String qualityType = support.qualityType() == null ? "Default" : support.qualityType();
        String enabled = Boolean.TRUE.equals(support.enabled()) ? "활성" : "비활성";
        String awakened = Boolean.TRUE.equals(support.awakened()) ? ", 각성" : "";
        return support.name() + " (레벨 " + level + ", 품질 " + quality + ", " + qualityType + ", " + enabled + awakened + ")";
    }

    private String skillProfile(List<String> values) {
        Set<String> tags = new java.util.HashSet<>(safe(values));
        List<String> profile = new ArrayList<>();
        if (tags.contains("damage-over-time")) profile.add("지속 피해");
        for (String element : List.of("fire", "cold", "lightning", "chaos")) {
            if (tags.contains(element)) profile.add(Map.of("fire", "화염", "cold", "냉기", "lightning", "번개", "chaos", "카오스").get(element));
        }
        if (tags.contains("projectile")) profile.add("투사체");
        if (tags.contains("area")) profile.add("범위");
        if (tags.contains("minion")) profile.add("소환수");
        if (profile.isEmpty() && tags.contains("attack")) profile.add("공격");
        if (profile.isEmpty() && tags.contains("spell")) profile.add("주문");
        return profile.isEmpty() ? "피해" : String.join("·", profile);
    }

    private boolean hasAny(Set<String> tags, String... candidates) {
        for (String candidate : candidates) if (tags.contains(candidate)) return true;
        return false;
    }

    private String supportSuffix(Set<String> tags, String... matchingTags) {
        return hasAny(tags, matchingTags) ? " 패시브와 장비 효과 태그가 이 방어 축을 보강합니다." : "";
    }

    private String resourceProfile(Set<String> tags) {
        return defenceProfile(tags, "life", "생명력", "energy-shield", "에너지 보호막");
    }

    private String damageProfile(Set<String> tags) {
        return profile(tags, "damage-over-time", "지속 피해", "fire", "화염", "cold", "냉기", "lightning", "번개", "chaos", "카오스",
                "physical", "물리", "attack", "공격", "spell", "주문", "minion", "소환수");
    }

    private String survivalProfile(Set<String> tags) {
        return profile(tags, "life", "생명력", "energy-shield", "에너지 보호막", "life-regeneration", "생명력 회복", "energy-shield-recovery", "에너지 보호막 회복",
                "armour", "방어도", "evasion", "회피", "ward", "와드", "fire-resistance", "화염 저항", "cold-resistance", "냉기 저항",
                "lightning-resistance", "번개 저항", "chaos-resistance", "카오스 저항");
    }

    private String hitDefenceProfile(Set<String> tags) {
        return profile(tags, "block", "공격 막기", "spell-block", "주문 막기", "spell-suppression", "주문 억제", "attack-dodge", "공격 회피",
                "spell-dodge", "주문 회피", "damage-avoidance", "피해 회피", "guard", "가드");
    }

    private String utilityProfile(Set<String> tags) {
        return profile(tags, "shock-immunity", "감전 면역", "shock-avoidance", "감전 회피", "freeze-immunity", "동결 면역", "chill-immunity", "냉각 면역",
                "ignite-immunity", "점화 면역", "armour", "방어도", "evasion", "회피", "life-regeneration", "생명력 회복",
                "block", "공격 막기", "spell-block", "주문 막기", "spell-suppression", "주문 억제", "fire-resistance", "화염 저항",
                "cold-resistance", "냉기 저항", "lightning-resistance", "번개 저항", "chaos-resistance", "카오스 저항");
    }

    private String utilityEffect(Set<String> tags) {
        boolean ailment = hasAny(tags, "shock-immunity", "shock-avoidance", "freeze-immunity", "chill-immunity", "ignite-immunity");
        boolean defence = hasAny(tags, "armour", "evasion", "life-regeneration", "block", "spell-block", "spell-suppression", "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance");
        if (ailment && defence) return "상태 이상 방지와 방어 수치";
        if (ailment) return "상태 이상 방지";
        return "방어 수치";
    }

    private String defenceProfile(Set<String> tags, String... values) {
        return profile(tags, values);
    }

    private String profile(Set<String> tags, String... values) {
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < values.length; index += 2) if (tags.contains(values[index])) labels.add(values[index + 1]);
        return labels.isEmpty() ? "방어" : String.join("·", labels);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
