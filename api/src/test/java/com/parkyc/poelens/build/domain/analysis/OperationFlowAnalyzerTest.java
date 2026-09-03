package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.OffenceFact;
import com.parkyc.poelens.build.domain.dto.OperationFact;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationFlowAnalyzerTest {

    private final OperationFlowAnalyzer analyzer = new OperationFlowAnalyzer();
    private final OffenceFact primaryAttack = new OffenceFact("Any Primary Attack", "primary", 1.0, "self-cast", List.of(), List.of());

    @Test
    void createsAResourceCycleOnlyForMatchingSubjects() {
        OperationFact consumesFrenzy = new OperationFact("skill", "Any Primary Attack", "consume", "frenzy-charge", List.of("Consumes a Frenzy Charge"));
        OperationFact gainsFrenzy = new OperationFact("item", "Any Item", "gain", "frenzy-charge", List.of("Gain a Frenzy Charge"));

        assertThat(analyzer.analyse(List.of(primaryAttack), List.of(consumesFrenzy, gainsFrenzy)))
                .contains(new OperationFlow("frenzy-charge", List.of("frenzy-charge 소비", "frenzy-charge 획득", "연속 사용"), List.of(consumesFrenzy, gainsFrenzy)));
    }

    @Test
    void linksGenericConditionsToThePrimaryOffenceAction() {
        OperationFact onKillExplosion = new OperationFact("passive", "Any Passive", "on-kill", "explosion", List.of("Explode on Kill"));
        OperationFact onHitEffect = new OperationFact("item", "Any Item", "on-hit", "shock", List.of("Effect on Hit"));
        OperationFact onDamagedEffect = new OperationFact("buff", "Any Buff", "on-damaged", "barrier", List.of("Effect when Damaged"));

        assertThat(analyzer.analyse(List.of(primaryAttack), List.of(onKillExplosion, onHitEffect, onDamagedEffect)))
                .contains(
                        new OperationFlow("Any Primary Attack", List.of("피해", "처치", "후속 효과"), List.of(onKillExplosion)),
                        new OperationFlow("Any Primary Attack", List.of("피해", "명중", "후속 효과"), List.of(onHitEffect)),
                        new OperationFlow("Any Primary Attack", List.of("피해", "피격", "후속 효과"), List.of(onDamagedEffect)));
    }

    @Test
    void emitsStandaloneFlowsForOperationalConstraintsAndEffects() {
        OperationFact maintain = new OperationFact("buff", "Any Buff", "maintain", "persistent-effect", List.of("Maintained effect"));
        OperationFact reserve = new OperationFact("skill", "Any Skill", "reserve", "mana", List.of("Mana Reservation"));
        OperationFact cooldown = new OperationFact("skill", "Any Skill", "cooldown", "skill-use", List.of("Cooldown"));
        OperationFact convert = new OperationFact("passive", "Any Passive", "convert", "physical-to-fire", List.of("Damage Conversion"));
        OperationFact enhance = new OperationFact("item", "Any Item", "enhance", "spell", List.of("Spell Damage"));

        assertThat(analyzer.analyse(List.of(primaryAttack), List.of(maintain, reserve, cooldown, convert, enhance)))
                .contains(
                        new OperationFlow("persistent-effect", List.of("persistent-effect 유지"), List.of(maintain)),
                        new OperationFlow("mana", List.of("mana 예약"), List.of(reserve)),
                        new OperationFlow("skill-use", List.of("skill-use 재사용 대기시간"), List.of(cooldown)),
                        new OperationFlow("physical-to-fire", List.of("physical-to-fire 전환"), List.of(convert)),
                        new OperationFlow("spell", List.of("spell 강화"), List.of(enhance)));
    }

    @Test
    void doesNotCreateFlowsWithoutTheirRequiredFacts() {
        OperationFact onlyConsume = new OperationFact("skill", "Any Skill", "consume", "frenzy-charge", List.of());

        assertThat(analyzer.analyse(null, null)).isEmpty();
        assertThat(analyzer.analyse(List.of(primaryAttack), List.of(onlyConsume))).isEmpty();
    }
}
