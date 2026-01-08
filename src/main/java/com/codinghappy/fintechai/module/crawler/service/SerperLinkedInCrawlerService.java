package com.codinghappy.fintechai.module.crawler.service;

import com.codinghappy.fintechai.common.constant.SystemConstant;
import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SerperLinkedInCrawlerService implements CrawlerService {

    private final RestTemplate restTemplate; // 移除 new RestTemplate()，使用 Spring 注入的带超时的实例
    private final ObjectMapper objectMapper;

    @Value("${serper.api-key}")
    private String serperApiKey;

    /**
     * 核心实现：通过 Serper 搜索关键词
     */
    @Override
    public List<CompanyProfileDTO> searchCompanies(String keyword, int limit) {
        log.info("🔍 Serper 搜索指令: {}, 限制: {}", keyword, limit);

        String url = "https://google.serper.dev/search";
        // 构造 Google 搜索语法：site:linkedin.com/company "关键词"
        // 这样能确保搜出来的都是领英公司主页
        String searchQuery = "site:linkedin.com/company " + keyword;

        return callSerperApi(searchQuery, limit);
    }

    /**
     * 核心修复：伪装成“抓取”。
     * 当系统要求抓取某个 URL 时，我们让 Serper 去搜这个 URL，从而获取它的简介。
     * 这样既规避了反爬虫，又能拿到数据。
     */
    @Override
    public CompanyProfileDTO crawlCompany(String url) {
        log.info("🕷️ 正在通过 Serper '抓取' (搜索) URL: {}", url);

        // 直接搜 URL，Google 通常第一条就是它，且带简介
        List<CompanyProfileDTO> results = callSerperApi(url, 1);

        if (!results.isEmpty()) {
            CompanyProfileDTO dto = results.get(0);
            // 修正：搜索 URL 时，link 应该就是 URL 本身
            dto.setLinkedinUrl(url);
            return dto;
        }
        return null;
    }

    /**
     * 批量抓取
     */
    @Override
    public List<CompanyProfileDTO> batchCrawl(List<String> urls) {
        List<CompanyProfileDTO> results = new ArrayList<>();
        for (String url : urls) {
            CompanyProfileDTO dto = crawlCompany(url);
            if (dto != null) {
                results.add(dto);
            }
            // 稍微歇一下，虽然 Serper 是 API，但也没必要并发太猛
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        return results;
    }

    /**
     * ❌ 之前的致命错误修正点
     * 旧代码: return url.contains("serper");
     * 后果: 传入 linkedin.com 时直接返回 false，导致跳过。
     */
    @Override
    public boolean supports(String url) {
        // ✅ 修正：只要是领英的链接，我都支持（通过搜 API 的方式）
        return url != null && url.contains("linkedin.com/");
    }

    @Override
    public String getDataSourceName() {
        return "SERPER_API";
    }

    // --- 私有方法：统一调用 Serper ---

    private List<CompanyProfileDTO> callSerperApi(String query, int limit) {
        String url = "https://google.serper.dev/search";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", serperApiKey);

        List<CompanyProfileDTO> results = new ArrayList<>();
        try {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("q", query);
            requestMap.put("num", limit);
            requestMap.put("gl", "cn"); // 可选：设置地理位置偏好 (cn, us, hk, sg)
            requestMap.put("hl", "zh-cn"); // 可选：设置语言偏好

            String jsonBody = objectMapper.writeValueAsString(requestMap);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // 使用 postForEntity 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                results = parseSerperResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("Serper API 调用异常: {}", e.getMessage());
        }
        return results;
    }

    private List<CompanyProfileDTO> parseSerperResponse(String body) {
        List<CompanyProfileDTO> dtos = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode organicResults = root.path("organic");

            if (organicResults.isArray()) {
                for (JsonNode node : organicResults) {
                    CompanyProfileDTO dto = new CompanyProfileDTO();

                    // 1. 清洗标题：去掉 " | LinkedIn" 后缀
                    String rawTitle = node.path("title").asText();
                    String cleanTitle = rawTitle.replaceAll(" \\| LinkedIn.*", "")
                            .replaceAll(" - LinkedIn.*", "");
                    dto.setCompanyName(cleanTitle);

                    // 2. 链接
                    dto.setLinkedinUrl(node.path("link").asText());

                    // 3. 简介 (Snippet) - 这是 DeepSeek 分析的核心原材料！
                    String snippet = node.path("snippet").asText();
                    dto.setDescription(snippet);

                    // 4. 补充默认值
                    dto.setDataSource(SystemConstant.DATA_SOURCE_LINKEDIN);
                    // 如果 snippet 为空，DeepSeek 可能会分析失败，这里给个默认值防止报错
                    if (snippet == null || snippet.isEmpty()) {
                        dto.setDescription(cleanTitle + " is a company listed on LinkedIn.");
                    }

                    dtos.add(dto);
                }
            }
        } catch (Exception e) {
            log.error("解析 Serper 响应失败", e);
        }
        return dtos;
    }
}