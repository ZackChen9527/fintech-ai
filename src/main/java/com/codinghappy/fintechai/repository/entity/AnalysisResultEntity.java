package com.codinghappy.fintechai.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
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

    @Column(name = "business_types", length = 500)
    private String businessTypes; // 格式: "CROSS_BORDER_PAYMENT,OVERSEAS_LOAN"

    @Column(name = "payment_willingness_score")
    private Integer paymentWillingnessScore;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "analysis_reason", columnDefinition = "TEXT")
    private String analysisReason;

    @Lob
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "analysis_model", length = 100)
    private String analysisModel;

    @Column(name = "processing_time_ms")
    private Double processingTimeMs;

    @Column(name = "analysis_time")
    private LocalDateTime analysisTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    // 辅助方法：检查是否为高质量分析结果
    public boolean isHighQuality() {
        return Boolean.TRUE.equals(success) &&
                confidence != null && confidence >= 0.7 &&
                paymentWillingnessScore != null && paymentWillingnessScore >= 7;
    }

    // 辅助方法：检查是否为低质量分析结果
    public boolean isLowQuality() {
        return Boolean.TRUE.equals(success) &&
                confidence != null && confidence < 0.7;
    }
}