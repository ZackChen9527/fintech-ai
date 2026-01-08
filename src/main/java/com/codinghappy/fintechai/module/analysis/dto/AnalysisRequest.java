package com.codinghappy.fintechai.module.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 前端请求参数封装
 */
@Data
public class AnalysisRequest {

    @NotNull(message = "公司ID不能为空")
    private Long companyId;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "公司简介不能为空")
    private String description;
}