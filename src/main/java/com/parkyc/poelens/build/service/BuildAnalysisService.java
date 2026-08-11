package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;

public interface BuildAnalysisService {

    AnalysisResult analyze(String pobInput);
}
