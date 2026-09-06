package com.parkyc.poelens.build.domain.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class MechanicProfileFormatter {

    boolean hasAny(Set<String> tags, String... candidates) {
        for (String candidate : candidates) if (tags.contains(candidate)) return true;
        return false;
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

    String profile(Set<String> tags, String... values) {
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < values.length; index += 2) if (tags.contains(values[index])) labels.add(values[index + 1]);
        return labels.isEmpty() ? "방어" : String.join("·", labels);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
