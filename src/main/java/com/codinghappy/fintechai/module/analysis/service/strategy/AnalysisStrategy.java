package com.codinghappy.fintechai.module.analysis.service.strategy;


import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.dto.BusinessType;


/**
 * 分析策略模式接口[citation:7]
 */
public interface AnalysisStrategy {

    /**
     * 判断是否适用于该业务类型
     */
    boolean supports(BusinessType businessType);

    /**
     * 分析公司描述
     */
    AnalysisResult analyze(String companyDescription);

    /**
     * 获取策略权重
     */
    default double getWeight() {
        return 1.0;
    }
}