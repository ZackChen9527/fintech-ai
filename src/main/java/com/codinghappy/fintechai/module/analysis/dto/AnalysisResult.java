package com.codinghappy.fintechai.module.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResult {

    /** 分析是否成功 */
    private boolean success;

    /** 错误信息（如果失败） */
    private String errorMessage;

    /** 识别的业务类型列表 */
    @Builder.Default
    private List<BusinessType> businessTypes = new ArrayList<>();

    /** 付费意愿评分（1-10） */
    private Integer paymentWillingnessScore;

    /** 分析置信度（0-1） */
    private Double confidence;

    /** 分析理由 */
    private String analysisReason;

    /** 原始API响应 */
    private String rawResponse;

    /** 分析时间 */
    @Builder.Default
    private LocalDateTime analysisTime = LocalDateTime.now();

    /**
     * 创建错误结果
     */
    public static AnalysisResult errorResult(String errorMessage) {
        return AnalysisResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .analysisTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建成功结果
     */
    public static AnalysisResult successResult(
            List<BusinessType> businessTypes,
            int score,
            double confidence,
            String reason) {
        return AnalysisResult.builder()
                .success(true)
                .businessTypes(businessTypes)
                .paymentWillingnessScore(score)
                .confidence(confidence)
                .analysisReason(reason)
                .analysisTime(LocalDateTime.now())
                .build();
    }
}