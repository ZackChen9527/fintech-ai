package com.codinghappy.fintechai.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analysis_result", indexes = {
        @Index(name = "idx_company_id", columnList = "company_id"),
        @Index(name = "idx_analysis_time", columnList = "analysis_time"),
        @Index(name = "idx_score", columnList = "payment_willingness_score"),
        @Index(name = "idx_confidence", columnList = "confidence"),
        @Index(name = "idx_success", columnList = "success"),
        @Index(name = "idx_model", columnList = "analysis_model")
})
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // 分析使用的模型 (如 deepseek-chat-v3)
    @Column(name = "analysis_model", length = 100)
    private String analysisModel;

    // 核心分析结果 (结构化的商业分析报告)
    @Column(name = "analysis_reason", columnDefinition = "TEXT")
    private String analysisReason;

    // 业务分类标签 (逗号分隔)
    @Column(name = "business_types", length = 500)
    private String businessTypes;

    // 置信度 (0.0 - 1.0)
    @Column(name = "confidence")
    private Double confidence;

    // 付费意愿评分 (1-10)
    @Column(name = "payment_willingness_score")
    private Integer paymentWillingnessScore;

    // 是否成功
    @Column(name = "success", nullable = false)
    private Boolean success;

    // 错误信息
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 接口耗时 (毫秒)
    @Column(name = "processing_time_ms")
    private Double processingTimeMs;

    // 原始响应数据 (用于调试和训练，务必保留)
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    // 版本号 (用于 Prompt 迭代管理)
    @Column(name = "version")
    private Integer version;

    @Column(name = "analysis_time")
    private LocalDateTime analysisTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}