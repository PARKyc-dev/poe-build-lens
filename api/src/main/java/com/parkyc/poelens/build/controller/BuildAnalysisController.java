package com.parkyc.poelens.build.controller;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.service.BuildAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class BuildAnalysisController {

    private final BuildAnalysisService buildAnalysisService;

    public BuildAnalysisController(BuildAnalysisService buildAnalysisService) {
        this.buildAnalysisService = buildAnalysisService;
    }

    @PostMapping
    public AnalysisResult analyze(@Valid @RequestBody BuildAnalysisRequest request) {
        return buildAnalysisService.analyze(request);
    }
}
