package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.application.NarrativeRefiner;
import com.parkyc.poelens.build.domain.analysis.EquipmentMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.OperationFlowAnalyzer;
import com.parkyc.poelens.build.domain.analysis.PassiveMechanicAnalyzer;
import com.parkyc.poelens.build.domain.analysis.PerformanceMechanicAnalyzer;
import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OperationFlow;
import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {
    private static final Logger log = LogManager.getLogger(BuildAnalysisServiceImpl.class);

    private final PassiveMechanicAnalyzer passiveAnalyzer;
    private final EquipmentMechanicAnalyzer equipmentAnalyzer;
    private final PerformanceMechanicAnalyzer performanceAnalyzer;
    private final OperationFlowAnalyzer operationFlowAnalyzer;
    private final NarrativeRefiner narrativeRefiner;

    public BuildAnalysisServiceImpl(PassiveMechanicAnalyzer passiveAnalyzer,
                                    EquipmentMechanicAnalyzer equipmentAnalyzer, PerformanceMechanicAnalyzer performanceAnalyzer,
                                    OperationFlowAnalyzer operationFlowAnalyzer, NarrativeRefiner narrativeRefiner) {
        this.passiveAnalyzer = passiveAnalyzer;
        this.equipmentAnalyzer = equipmentAnalyzer;
        this.performanceAnalyzer = performanceAnalyzer;
        this.operationFlowAnalyzer = operationFlowAnalyzer;
        this.narrativeRefiner = narrativeRefiner;
    }

    @Override
    public AnalysisResult analyze(BuildAnalysisRequest request) {
        long startedAt = System.nanoTime();
        BuildFacts facts = request.buildFacts();
        if (facts == null) {
            log.warn("빌드 사실이 없는 분석 요청: 게임 버전={}", request.gameVersion());
            throw new PoeLensException(ErrorCode.MISSING_BUILD_INPUT);
        }

        log.info("빌드 분석 시작: 게임 버전={}, 공격 사실 수={}, 방어 사실 수={}", request.gameVersion(), size(facts.offence()), size(facts.defence()));
        List<OperationFlow> operationFlows = operationFlowAnalyzer.analyse(facts.offence(), facts.operationFacts());
        NarrativeResult narrative = narrativeRefiner.refine(facts, operationFlows);
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
