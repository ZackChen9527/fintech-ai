package com.codinghappy.fintechai.module.crawler.service;

import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;

import java.util.List;

public interface CrawlerService {

    /**
     * 抓取公司信息
     */
    CompanyProfileDTO crawlCompany(String urlOrIdentifier);

    /**
     * 批量抓取
     */
    List<CompanyProfileDTO> batchCrawl(List<String> urlsOrIdentifiers);

    /**
     * 搜索公司
     */
    List<CompanyProfileDTO> searchCompanies(String keyword, int limit);

    /**
     * 验证URL是否支持
     */
    boolean supports(String url);

    /**
     * 获取数据源名称
     */
    String getDataSourceName();

    /**
     * 获取服务状态
     */
    default String getStatus() {
        return "正常";
    }
}