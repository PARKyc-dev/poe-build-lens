package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {

    private final BuildFactsAnalysisService buildFactsAnalysisService;

    public BuildAnalysisServiceImpl(BuildFactsAnalysisService buildFactsAnalysisService) {
        this.buildFactsAnalysisService = buildFactsAnalysisService;
    }

    @Override
    public AnalysisResult analyze(BuildAnalysisRequest request) {
        BuildFacts facts = request.buildFacts();
        if (facts == null) {
            return new AnalysisResult(request.gameVersion(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("No build facts are available for analysis."), List.of());
        }

        List<Mechanic> offence = new java.util.ArrayList<>(buildFactsAnalysisService.analyseOffence(facts.offence(), facts.items()));
        List<Mechanic> buffs = new java.util.ArrayList<>(buildFactsAnalysisService.analyseBuffs(facts.buffs()));
        buffs.addAll(buildFactsAnalysisService.analyseMobility(facts.mobility()));
        return new AnalysisResult(request.gameVersion(),
                offence,
                buildFactsAnalysisService.analyseDefence(facts.defence(), facts.passives(), facts.passiveTags(), facts.items()),
                buffs,
                buildFactsAnalysisService.analysePassives(facts.passives(), facts.passiveTags()),
                List.of(), List.of(), List.of());
    }
}
