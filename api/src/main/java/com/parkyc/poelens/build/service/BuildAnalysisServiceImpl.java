package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {
    private static final Logger log = LogManager.getLogger(BuildAnalysisServiceImpl.class);

    private final BuildFactsAnalysisService buildFactsAnalysisService;
    private final NarrativeRefiner narrativeRefiner;

    public BuildAnalysisServiceImpl(BuildFactsAnalysisService buildFactsAnalysisService, NarrativeRefiner narrativeRefiner) {
        this.buildFactsAnalysisService = buildFactsAnalysisService;
        this.narrativeRefiner = narrativeRefiner;
    }

    @Override
    public AnalysisResult analyze(BuildAnalysisRequest request) {
        long startedAt = System.nanoTime();
        BuildFacts facts = request.buildFacts();
        if (facts == null) {
            log.warn("빌드 사실이 없는 분석 요청: 게임 버전={}", request.gameVersion());
            return new AnalysisResult(request.gameVersion(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("No build facts are available for analysis."), List.of());
        }

        log.info("빌드 분석 시작: 게임 버전={}, 공격 사실 수={}, 방어 사실 수={}", request.gameVersion(), size(facts.offence()), size(facts.defence()));
        List<Mechanic> offence = new java.util.ArrayList<>(buildFactsAnalysisService.analyseOffenceNarrative(facts.offence(), facts.ascendancies()));
        List<Mechanic> defence = buildFactsAnalysisService.analyseDefenceNarrative(facts.defence(), facts.passives(), facts.buffs());
        List<Mechanic> buffs = buildFactsAnalysisService.analyseBuffs(facts.buffs());
        List<Mechanic> mobility = buildFactsAnalysisService.analyseMobility(facts.mobility());
        List<List<Mechanic>> narrative = narrativeRefiner.refine(facts, offence, defence, buffs, mobility);
        AnalysisResult result = new AnalysisResult(request.gameVersion(),
                narrative.get(0), narrative.get(1),
                narrative.get(2),
                narrative.get(3),
                buildFactsAnalysisService.analysePassives(facts.passives(), facts.passiveTags()),
                buildFactsAnalysisService.analysePassiveNodes(facts.passives()),
                buildFactsAnalysisService.analyseAscendancies(facts.ascendancies()),
                buildFactsAnalysisService.analyseGear(facts.items(), facts.jewels()),
                buildFactsAnalysisService.analysePerformance(facts.performance()),
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
