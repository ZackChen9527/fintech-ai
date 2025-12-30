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
@Primary // 加上这个，Spring 就会默认注入这个真实的抓取服务
@RequiredArgsConstructor
public class SerperLinkedInCrawlerService implements CrawlerService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${serper.api-key}") // 在 application.yml 中配置
    private String serperApiKey;

    @Override
    public List<CompanyProfileDTO> searchCompanies(String keyword, int limit) {
        log.info("通过 Serper 搜索 LinkedIn 公司: {}, 限制: {}", keyword, limit);

        String url = "https://google.serper.dev/search";
        String searchQuery = "site:linkedin.com/company \"" + keyword + "\"";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", serperApiKey);

        List<CompanyProfileDTO> results = new ArrayList<>();
        try {
            // --- 修复开始：使用 Map + ObjectMapper 自动处理转义 ---
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("q", searchQuery);
            requestMap.put("num", limit);

            String jsonBody = objectMapper.writeValueAsString(requestMap);
            // ------------------------------------------------

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                results = parseSerperResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("Serper API 调用失败", e);
        }
        return results;
    }

    private List<CompanyProfileDTO> parseSerperResponse(String body) throws Exception {
        List<CompanyProfileDTO> dtos = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode organicResults = root.path("organic");

        for (JsonNode node : organicResults) {
            CompanyProfileDTO dto = new CompanyProfileDTO();
            dto.setCompanyName(node.path("title").asText().replace(" | LinkedIn", ""));
            dto.setLinkedinUrl(node.path("link").asText());
            dto.setDescription(node.path("snippet").asText()); // 这就是 AI 分析的关键：公司简介
            dto.setDataSource(SystemConstant.DATA_SOURCE_LINKEDIN);
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public CompanyProfileDTO crawlCompany(String url) { /* 可选实现，主要用 search */ return null; }

    @Override
    public List<CompanyProfileDTO> batchCrawl(List<String> urls) { return new ArrayList<>(); }

    @Override
    public boolean supports(String url) { return url.contains("serper"); }

    @Override
    public String getDataSourceName() { return "SERPER_LINKEDIN"; }
}