package com.codinghappy.fintechai.module.crawler.controller;

import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import com.codinghappy.fintechai.module.crawler.service.CrawlerService;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/crawler")
@RequiredArgsConstructor
public class CrawlerController {

//    private final LinkedInCrawlerService crawlerService;

    // 修改后：注入接口
    private final CrawlerService crawlerService;

    // 1. 在 Controller 中注入 Repository
    private final CompanyRepository companyRepository;

    @PostMapping("/linkedin/single")
    public ResponseEntity<CompanyProfileDTO> crawlLinkedInCompany(
            @RequestParam @NotBlank String linkedinUrl) {
        try {
            CompanyProfileDTO result = crawlerService.crawlCompany(linkedinUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("抓取LinkedIn公司失败: {}", linkedinUrl, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/linkedin/batch")
    public ResponseEntity<List<CompanyProfileDTO>> batchCrawlLinkedIn(
            @RequestBody List<String> linkedinUrls) {
        try {
            List<CompanyProfileDTO> results = crawlerService.batchCrawl(linkedinUrls);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("批量抓取失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/linkedin/search")
    public ResponseEntity<List<CompanyProfileDTO>> searchCompanies(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 抓取数据
            List<CompanyProfileDTO> results = crawlerService.searchCompanies(keyword, limit);

            // --- 核心修复：将 DTO 转换为 Entity 并保存到数据库 ---
            // 假设你有一个方法将 DTO 转为 Company 实体
            List<CompanyEntity> companies = results.stream().map(dto -> {
                CompanyEntity entity = new CompanyEntity();
                entity.setName(dto.getCompanyName());
                entity.setDescription(dto.getDescription());
                entity.setLinkedinUrl(dto.getLinkedinUrl());
                entity.setDataSource(dto.getDataSource());
                entity.setIsActive(true);
                return entity;
            }).toList();

            companyRepository.saveAll(companies); // 存入 MySQL
            // ----------------------------------------------

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("搜索公司失败: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Crawler service is healthy");
    }
}