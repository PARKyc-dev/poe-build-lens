package com.parkyc.poelens.build.controller;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;
import com.parkyc.poelens.build.service.BuildAnalysisService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class BuildAnalysisController {
    private static final Logger log = LogManager.getLogger(BuildAnalysisController.class);

    private final BuildAnalysisService buildAnalysisService;

    public BuildAnalysisController(BuildAnalysisService buildAnalysisService) {
        this.buildAnalysisService = buildAnalysisService;
    }

    @PostMapping
    public AnalysisResult analyze(@Valid @RequestBody BuildAnalysisRequest request) {
        log.info("빌드 분석 요청 수신: 게임 버전={}, 빌드 사실 포함={}", request.gameVersion(), request.buildFacts() != null);
        return buildAnalysisService.analyze(request);
    }
}
