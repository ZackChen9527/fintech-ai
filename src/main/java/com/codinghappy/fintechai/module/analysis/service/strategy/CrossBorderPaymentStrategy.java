package com.codinghappy.fintechai.module.analysis.service.strategy;

import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.dto.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class CrossBorderPaymentStrategy implements AnalysisStrategy {

    private static final List<String> KEYWORDS = Arrays.asList(
            "跨境支付", "国际支付", "跨境结算", "外汇支付",
            "跨境收款", "国际汇款", "货币兑换", "外汇交易",
            "跨境金融", "国际结算", "SWIFT", "跨境资金"
    );

    private static final List<String> NEGATIVE_KEYWORDS = Arrays.asList(
            "国内支付", "境内支付", "本地支付", "人民币支付"
    );

    @Override
    public boolean supports(BusinessType businessType) {
        return BusinessType.CROSS_BORDER_PAYMENT.equals(businessType);
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
                        "跨境支付关键词匹配%d个，否定词%d个，计算得分%.1f",
                        keywordCount, negativeCount, score
                ))
                .build();
    }

    @Override
    public double getWeight() {
        return 1.2; // 跨境支付策略权重较高
    }

    private double calculateScore(int keywordCount, int negativeCount) {
        double baseScore = Math.min(keywordCount * 2.5, 8.0);
        double penalty = negativeCount * 1.5;
        return Math.max(1.0, baseScore - penalty);
    }

    private double calculateConfidence(int keywordCount, int negativeCount) {
        double confidence = 0.3 + (keywordCount * 0.15) - (negativeCount * 0.1);
        return Math.max(0.1, Math.min(0.95, confidence));
    }
}