package com.codinghappy.fintechai.module.analysis.service.strategy;


import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;

import com.codinghappy.fintechai.module.analysis.dto.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class OverseasLoanStrategy implements AnalysisStrategy {

    private static final List<String> KEYWORDS = Arrays.asList(
            "海外借贷", "国际贷款", "跨境融资", "海外融资",
            "境外贷款", "国际信贷", "跨境借贷", "海外债权",
            "国际保理", "出口信贷", "项目融资", "跨境担保"
    );

    private static final List<String> NEGATIVE_KEYWORDS = Arrays.asList(
            "国内贷款", "境内融资", "本地借贷", "消费贷款"
    );

    @Override
    public boolean supports(BusinessType businessType) {
        return BusinessType.OVERSEAS_LOAN.equals(businessType);
    }

    @Override
    public AnalysisResult analyze(String companyDescription) {
        String lowerDesc = companyDescription.toLowerCase();
        int keywordCount = 0;
        int negativeCount = 0;

        // 统计关键词出现次数
        for (String keyword : KEYWORDS) {
            if (lowerDesc.contains(keyword.toLowerCase())) {
                keywordCount++;
            }
        }

        // 统计否定关键词
        for (String negative : NEGATIVE_KEYWORDS) {
            if (lowerDesc.contains(negative.toLowerCase())) {
                negativeCount++;
            }
        }

        // 计算得分
        double score = calculateScore(keywordCount, negativeCount);

        return AnalysisResult.builder()
                .success(true)
                .paymentWillingnessScore((int) Math.min(10, Math.round(score)))
                .confidence(calculateConfidence(keywordCount, negativeCount))
                .analysisReason(String.format(
                        "海外借贷关键词匹配%d个，否定词%d个，计算得分%.1f",
                        keywordCount, negativeCount, score
                ))
                .build();
    }

    @Override
    public double getWeight() {
        return 1.0; // 标准权重
    }

    private double calculateScore(int keywordCount, int negativeCount) {
        double baseScore = Math.min(keywordCount * 2.2, 7.5);
        double penalty = negativeCount * 1.2;
        return Math.max(1.0, baseScore - penalty);
    }

    private double calculateConfidence(int keywordCount, int negativeCount) {
        double confidence = 0.25 + (keywordCount * 0.12) - (negativeCount * 0.08);
        return Math.max(0.1, Math.min(0.9, confidence));
    }
}