package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.SupportGemFact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MechanicProfileFormatter {

    private static final Map<String, String> DELIVERY_TITLES = Map.of(
            "self-cast", "직접 시전 (Self-Cast)",
            "attack", "공격 (Attack)",
            "totem", "토템 (Totem)",
            "trap", "트랩 (Trap)",
            "mine", "마인 (Mine)",
            "minion", "소환수 (Minion)",
            "brand", "브랜드 (Brand)",
            "trigger", "트리거 (Trigger)",
            "persistent", "상시 유지형 피해 (Persistent)");

    String deliveryTitle(String delivery) {
        return DELIVERY_TITLES.getOrDefault(delivery, "확인 불가 (Unverified)");
    }

    String supportDescription(SupportGemFact support) {
        String level = support.level() == null ? "?" : support.level().toString();
        String quality = support.quality() == null ? "?" : support.quality().toString();
        String qualityType = support.qualityType() == null ? "Default" : support.qualityType();
        String enabled = Boolean.TRUE.equals(support.enabled()) ? "활성" : "비활성";
        String awakened = Boolean.TRUE.equals(support.awakened()) ? ", 각성" : "";
        return support.name() + " (레벨 " + level + ", 품질 " + quality + ", " + qualityType + ", " + enabled + awakened + ")";
    }

    String skillProfile(List<String> values) {
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

    boolean hasAny(Set<String> tags, String... candidates) {
        for (String candidate : candidates) if (tags.contains(candidate)) return true;
        return false;
    }

    String supportSuffix(Set<String> tags, String... matchingTags) {
        return hasAny(tags, matchingTags) ? " 패시브와 장비 효과 태그가 이 방어 축을 보강합니다." : "";
    }

    String resourceProfile(Set<String> tags) {
        return defenceProfile(tags, "life", "생명력", "energy-shield", "에너지 보호막");
    }

    String damageProfile(Set<String> tags) {
        return profile(tags, "damage-over-time", "지속 피해", "fire", "화염", "cold", "냉기", "lightning", "번개", "chaos", "카오스",
                "physical", "물리", "attack", "공격", "spell", "주문", "minion", "소환수");
    }

    String survivalProfile(Set<String> tags) {
        return profile(tags, "life", "생명력", "energy-shield", "에너지 보호막", "life-regeneration", "생명력 회복", "energy-shield-recovery", "에너지 보호막 회복",
                "armour", "방어도", "evasion", "회피", "ward", "와드", "fire-resistance", "화염 저항", "cold-resistance", "냉기 저항",
                "lightning-resistance", "번개 저항", "chaos-resistance", "카오스 저항");
    }

    String hitDefenceProfile(Set<String> tags) {
        return profile(tags, "block", "공격 막기", "spell-block", "주문 막기", "spell-suppression", "주문 억제", "attack-dodge", "공격 회피",
                "spell-dodge", "주문 회피", "damage-avoidance", "피해 회피", "guard", "가드");
    }

    String utilityProfile(Set<String> tags) {
        return profile(tags, "shock-immunity", "감전 면역", "shock-avoidance", "감전 회피", "freeze-immunity", "동결 면역", "chill-immunity", "냉각 면역",
                "ignite-immunity", "점화 면역", "armour", "방어도", "evasion", "회피", "life-regeneration", "생명력 회복",
                "block", "공격 막기", "spell-block", "주문 막기", "spell-suppression", "주문 억제", "fire-resistance", "화염 저항",
                "cold-resistance", "냉기 저항", "lightning-resistance", "번개 저항", "chaos-resistance", "카오스 저항");
    }

    String utilityEffect(Set<String> tags) {
        boolean ailment = hasAny(tags, "shock-immunity", "shock-avoidance", "freeze-immunity", "chill-immunity", "ignite-immunity");
        boolean defence = hasAny(tags, "armour", "evasion", "life-regeneration", "block", "spell-block", "spell-suppression", "fire-resistance", "cold-resistance", "lightning-resistance", "chaos-resistance");
        if (ailment && defence) return "상태 이상 방지와 방어 수치";
        if (ailment) return "상태 이상 방지";
        return "방어 수치";
    }

    String defenceProfile(Set<String> tags, String... values) {
        return profile(tags, values);
    }

    String profile(Set<String> tags, String... values) {
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < values.length; index += 2) if (tags.contains(values[index])) labels.add(values[index + 1]);
        return labels.isEmpty() ? "방어" : String.join("·", labels);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
