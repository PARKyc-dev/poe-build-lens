package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.Evidence;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.PobBuild;
import com.parkyc.poelens.build.parser.PobParser;
import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.config.exception.PoeLensException;
import com.parkyc.poelens.mechanics.service.MechanicService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {

    private final PobParser pobParser;
    private final MechanicService mechanicService;

    public BuildAnalysisServiceImpl(PobParser pobParser, MechanicService mechanicService) {
        this.pobParser = pobParser;
        this.mechanicService = mechanicService;
    }

    @Override
    public AnalysisResult analyze(String pobInput) {
        if (pobInput == null || pobInput.isBlank()) {
            throw new PoeLensException(ErrorCode.MISSING_BUILD_INPUT);
        }

        PobBuild build = pobParser.parse(pobInput);
        if (build.gameVersion() != null && !mechanicService.gameVersion().equals(build.gameVersion())) {
            throw new PoeLensException(ErrorCode.UNSUPPORTED_GAME_VERSION);
        }

        List<Mechanic> interactions = new ArrayList<>();
        List<Evidence> evidence = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        for (String gem : build.gems()) {
            mechanicService.find(gem).ifPresentOrElse(entry -> {
                interactions.add(new Mechanic(entry.getTitle(), entry.getExplanation()));
                evidence.add(new Evidence(entry.getName(), entry.getSourceUrl(), entry.getCollectedAt().toString(), entry.isReviewed()));
            }, () -> unverified.add(gem));
        }

        String mainSkill = build.gems().isEmpty() ? "no active skill" : build.gems().getFirst();
        String overview = "Level " + build.level() + " " + build.characterClass() + " using " + mainSkill;
        return new AnalysisResult(mechanicService.gameVersion(), overview, interactions, build.gems(), build.items(),
                List.of("No defence interaction is verified by the local catalog yet."),
                List.of("No resource-sustain interaction is verified by the local catalog yet."), unverified, evidence);
    }
}
