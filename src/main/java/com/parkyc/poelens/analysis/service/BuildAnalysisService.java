package com.parkyc.poelens.analysis.service;

import com.parkyc.poelens.analysis.catalog.MechanicsCatalog;
import com.parkyc.poelens.analysis.exception.AnalysisException;
import com.parkyc.poelens.analysis.model.AnalysisResult;
import com.parkyc.poelens.analysis.model.Evidence;
import com.parkyc.poelens.analysis.model.Mechanic;
import com.parkyc.poelens.analysis.model.PobBuild;
import com.parkyc.poelens.analysis.parser.PobParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BuildAnalysisService {

    private final PobParser pobParser;
    private final MechanicsCatalog catalog;

    public BuildAnalysisService(PobParser pobParser, MechanicsCatalog catalog) {
        this.pobParser = pobParser;
        this.catalog = catalog;
    }

    public AnalysisResult analyze(String pobInput) {
        if (pobInput == null || pobInput.isBlank()) {
            throw new AnalysisException("MISSING_BUILD_INPUT", "PoB build input is required.");
        }

        PobBuild build = pobParser.parse(pobInput);
        if (build.gameVersion() != null && !catalog.gameVersion().equals(build.gameVersion())) {
            throw new AnalysisException("UNSUPPORTED_GAME_VERSION", "This build does not match the local mechanics catalog version.");
        }

        List<Mechanic> interactions = new ArrayList<>();
        List<Evidence> evidence = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        for (String gem : build.gems()) {
            catalog.find(gem).ifPresentOrElse(entry -> {
                interactions.add(new Mechanic(entry.getTitle(), entry.getExplanation()));
                evidence.add(new Evidence(entry.getName(), entry.getSourceUrl(), entry.getCollectedAt().toString(), entry.isReviewed()));
            }, () -> unverified.add(gem));
        }

        String mainSkill = build.gems().isEmpty() ? "no active skill" : build.gems().getFirst();
        String overview = "Level " + build.level() + " " + build.characterClass() + " using " + mainSkill;
        return new AnalysisResult(catalog.gameVersion(), overview, interactions, build.gems(), build.items(),
                List.of("No defence interaction is verified by the local catalog yet."),
                List.of("No resource-sustain interaction is verified by the local catalog yet."),
                unverified, evidence);
    }
}
