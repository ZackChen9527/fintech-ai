package com.codinghappy.fintechai.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_name", columnList = "name"),
        @Index(name = "idx_company_industry", columnList = "industry"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_is_active", columnList = "isActive")
})
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "revenue_range", length = 100)
    private String revenueRange;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "headquarters", length = 200)
    private String headquarters;

    @Column(name = "specialities", columnDefinition = "TEXT")
    private String specialities;

    @Column(name = "data_source", length = 50)
    private String dataSource;

    @Column(name = "crawl_time")
    private LocalDateTime crawlTime;

    @Column(name = "crawl_status", length = 50)
    private String crawlStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    // 为了方便查询，添加一个方法检查公司是否有效
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) &&
                name != null && !name.trim().isEmpty() &&
                description != null && !description.trim().isEmpty();
    }
}