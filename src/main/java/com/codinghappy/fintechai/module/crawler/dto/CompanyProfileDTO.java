package com.codinghappy.fintechai.module.crawler.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyProfileDTO {

    private String companyName;

    private String description;

    private String website;

    private String linkedinUrl;

    private String location;

    private String companySize;

    private String industry;

    private String tags;

    private Integer employeeCount;

    private Integer foundedYear;

    private String revenueRange;

    private String contactEmail;

    private String contactPhone;

    private String headquarters;

    private String specialities;

    private String dataSource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date crawlTime;

    private String crawlStatus;

    private String errorMessage;

    private Double confidenceScore;

    // 以下为LinkedIn特有字段
    private String linkedinFollowers;

    private String linkedinUpdates;

    private String companyType;

    private String linkedinCompanyId;

    private String companyLogoUrl;

    private String companyBackgroundImageUrl;

    // 验证方法
    public boolean isValid() {
        return companyName != null && !companyName.trim().isEmpty() &&
                description != null && description.length() >= 10;
    }

    public String getSummary() {
        return String.format("%s | %s | %s | %s",
                companyName,
                industry != null ? industry : "未知行业",
                location != null ? location : "未知地区",
                companySize != null ? companySize : "未知规模");
    }
}