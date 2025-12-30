package com.codinghappy.fintechai.module.crawler.controller;

import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import com.codinghappy.fintechai.module.crawler.service.LinkedInCrawlerService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final LinkedInCrawlerService crawlerService;

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
            List<CompanyProfileDTO> results = crawlerService.searchCompanies(keyword, limit);
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