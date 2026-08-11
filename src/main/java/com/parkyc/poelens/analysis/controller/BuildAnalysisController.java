package com.parkyc.poelens.analysis.controller;

import com.parkyc.poelens.analysis.exception.AnalysisException;
import com.parkyc.poelens.analysis.model.AnalysisResult;
import com.parkyc.poelens.analysis.service.BuildAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
class BuildAnalysisController {

    private final BuildAnalysisService analysisService;

    BuildAnalysisController(BuildAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    AnalysisResult analyze(@RequestBody AnalysisRequest request) {
        return analysisService.analyze(request.pobInput());
    }

    @ExceptionHandler(AnalysisException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handle(AnalysisException exception) {
        return new ApiError(exception.code(), exception.getMessage());
    }

    record AnalysisRequest(String pobInput) {
    }

    record ApiError(String code, String message) {
    }
}
