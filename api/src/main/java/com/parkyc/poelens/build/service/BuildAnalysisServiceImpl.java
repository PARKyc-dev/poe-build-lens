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
    private final LocalNarrativeService localNarrativeService;

    public BuildAnalysisServiceImpl(BuildFactsAnalysisService buildFactsAnalysisService, LocalNarrativeService localNarrativeService) {
        this.buildFactsAnalysisService = buildFactsAnalysisService;
        this.localNarrativeService = localNarrativeService;
    }

    @Override
    public AnalysisResult analyze(BuildAnalysisRequest request) {
        BuildFacts facts = request.buildFacts();
        if (facts == null) {
            return new AnalysisResult(request.gameVersion(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("No build facts are available for analysis."), List.of());
        }

        List<Mechanic> offence = new java.util.ArrayList<>(buildFactsAnalysisService.analyseOffenceNarrative(facts.offence(), facts.ascendancies()));
        List<Mechanic> defence = buildFactsAnalysisService.analyseDefenceNarrative(facts.defence(), facts.passives(), facts.buffs());
        List<List<Mechanic>> narrative = localNarrativeService.refine(facts, offence, defence);
        List<Mechanic> buffs = new java.util.ArrayList<>(buildFactsAnalysisService.analyseBuffs(facts.buffs()));
        buffs.addAll(buildFactsAnalysisService.analyseMobility(facts.mobility()));
        return new AnalysisResult(request.gameVersion(),
                narrative.get(0), narrative.get(1),
                buffs,
                buildFactsAnalysisService.analysePassives(facts.passives(), facts.passiveTags()),
                buildFactsAnalysisService.analysePassiveNodes(facts.passives()),
                buildFactsAnalysisService.analyseAscendancies(facts.ascendancies()),
                buildFactsAnalysisService.analyseGear(facts.items(), facts.jewels()),
                buildFactsAnalysisService.analysePerformance(facts.performance()),
                List.of(), List.of(), List.of());
    }
}
