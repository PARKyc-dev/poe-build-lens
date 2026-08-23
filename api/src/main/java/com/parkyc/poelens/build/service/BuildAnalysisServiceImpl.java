package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.analysis.BuffMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.BuildSummaryGenerator;
import com.parkyc.poelens.build.domain.analysis.DefenceMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.EquipmentMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.OffenceMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.PassiveMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.PerformanceMechanicAnalyzer;
import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {
    private static final Logger log = LogManager.getLogger(BuildAnalysisServiceImpl.class);

    private final OffenceMechanicAnalyzer offenceAnalyzer;
    private final DefenceMechanicAnalyzer defenceAnalyzer;
    private final BuffMechanicAnalyzer buffAnalyzer;
    private final PassiveMechanicAnalyzer passiveAnalyzer;
    private final EquipmentMechanicAnalyzer equipmentAnalyzer;
    private final PerformanceMechanicAnalyzer performanceAnalyzer;
    private final BuildSummaryGenerator buildSummaryGenerator;
    private final NarrativeRefiner narrativeRefiner;

    public BuildAnalysisServiceImpl(OffenceMechanicAnalyzer offenceAnalyzer, DefenceMechanicAnalyzer defenceAnalyzer,
                                    BuffMechanicAnalyzer buffAnalyzer, PassiveMechanicAnalyzer passiveAnalyzer,
                                    EquipmentMechanicAnalyzer equipmentAnalyzer, PerformanceMechanicAnalyzer performanceAnalyzer,
                                    BuildSummaryGenerator buildSummaryGenerator, NarrativeRefiner narrativeRefiner) {
        this.offenceAnalyzer = offenceAnalyzer;
        this.defenceAnalyzer = defenceAnalyzer;
        this.buffAnalyzer = buffAnalyzer;
        this.passiveAnalyzer = passiveAnalyzer;
        this.equipmentAnalyzer = equipmentAnalyzer;
        this.performanceAnalyzer = performanceAnalyzer;
        this.buildSummaryGenerator = buildSummaryGenerator;
        this.narrativeRefiner = narrativeRefiner;
    }

    @Override
    public AnalysisResult analyze(BuildAnalysisRequest request) {
        long startedAt = System.nanoTime();
        BuildFacts facts = request.buildFacts();
        if (facts == null) {
            log.warn("빌드 사실이 없는 분석 요청: 게임 버전={}", request.gameVersion());
            return new AnalysisResult(request.gameVersion(), "PoB 사실이 없어 빌드 요약을 만들 수 없습니다.", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("No build facts are available for analysis."), List.of());
        }

        log.info("빌드 분석 시작: 게임 버전={}, 공격 사실 수={}, 방어 사실 수={}", request.gameVersion(), size(facts.offence()), size(facts.defence()));
        List<Mechanic> offence = new java.util.ArrayList<>(offenceAnalyzer.analyseNarrative(facts.offence(), facts.skills()));
        List<Mechanic> defence = defenceAnalyzer.analyse(facts.defence(), facts.passives(), facts.passiveTags(), facts.items());
        List<Mechanic> buffs = buffAnalyzer.analyse(facts.buffs());
        NarrativeResult narrative = narrativeRefiner.refine(facts, buildSummaryGenerator.generate(facts.offence(), facts.defence(), facts.buffs()), offence, defence, buffs);
        AnalysisResult result = new AnalysisResult(request.gameVersion(),
                narrative.summary(), narrative.offence(), narrative.defence(), narrative.buffs(),
                passiveAnalyzer.analyse(facts.passives(), facts.passiveTags()),
                passiveAnalyzer.analyseNodes(facts.passives()),
                passiveAnalyzer.analyseAscendancies(facts.ascendancies()),
                equipmentAnalyzer.analyse(facts.items(), facts.jewels()),
                performanceAnalyzer.analyse(facts.performance()),
                List.of(), List.of(), List.of());
        log.info("빌드 분석 완료: 게임 버전={}, 공격 결과 수={}, 방어 결과 수={}, 처리 시간(ms)={}", request.gameVersion(), result.offence().size(), result.defence().size(), elapsedMillis(startedAt));
        return result;
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
