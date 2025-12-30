package com.codinghappy.fintechai.module.analysis.service;

import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;

import java.util.List;

public interface AnalysisService {

    /**
     * 分析单个公司
     */
    AnalysisResult analyzeCompany(AnalysisRequest request);

    /**
     * 批量分析公司
     */
    List<AnalysisResult> batchAnalyze(List<AnalysisRequest> requests);

    /**
     * 验证公司描述是否有效
     */
    default boolean validateDescription(String description) {
        return description != null &&
                description.length() >= 10 &&
                description.length() <= 5000;
    }
}