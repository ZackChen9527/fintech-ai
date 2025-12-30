package com.codinghappy.fintechai.module.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import javax.validation.constraints.Size;

@Data
public class AnalysisRequest {

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /** LinkedIn公司简介 */
    @NotBlank(message = "公司简介不能为空")
    @Size(min = 10, max = 5000, message = "公司简介长度在10-5000字符之间")
    private String description;

    /** 公司网址 */
    private String website;

    /** 所在地区 */
    private String location;

    /** 公司规模 */
    private String companySize;

    /** 所属行业 */
    private String industry;
}