package com.parkyc.poelens.build.service;

import com.parkyc.poelens.build.domain.dto.AnalysisResult;
import com.parkyc.poelens.build.domain.dto.BuildAnalysisRequest;

public interface BuildAnalysisService {

    AnalysisResult analyze(BuildAnalysisRequest request);
}
