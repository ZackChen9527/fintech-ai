package com.codinghappy.fintechai.module.crawler.controller;

import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import com.codinghappy.fintechai.module.crawler.service.CrawlerService;
import com.codinghappy.fintechai.module.crawler.task.LinkedInCrawlerTask;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
// 新增导入
import com.codinghappy.fintechai.module.analysis.task.AnalysisTask;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    // 注入接口
    private final CrawlerService crawlerService;

    // 1. 在 Controller 中注入 Repository
    private final CompanyRepository companyRepository;

    // 2. ✅ 新增注入：分析任务 (印钞机开关)
    private final AnalysisTask analysisTask;

    private final LinkedInCrawlerTask linkedinCrawlerTask;

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

    /**
     * 核心接口：搜索 -> 抓取 -> 入库 -> 【自动触发分析】
     */
    @GetMapping("/linkedin/search")
    public ResponseEntity<List<CompanyProfileDTO>> searchCompanies(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("🔍 接到搜索指令: {}, 限制: {}", keyword, limit);

            // 1. 抓取数据 (这里调用 Serper API)
            List<CompanyProfileDTO> results = crawlerService.searchCompanies(keyword, limit);

            if (results.isEmpty()) {
                log.warn("未搜索到任何公司数据");
                return ResponseEntity.ok(results);
            }

            // 2. 将 DTO 转换为 Entity
            List<CompanyEntity> companies = results.stream().map(dto -> {
                CompanyEntity entity = new CompanyEntity();
                entity.setName(dto.getCompanyName());
                entity.setDescription(dto.getDescription());
                entity.setLinkedinUrl(dto.getLinkedinUrl());
                entity.setDataSource("LinkedIn_Search"); // 标记来源
                entity.setIsActive(true);
                // 这里可以补充 website 等其他字段的映射
                return entity;
            }).collect(Collectors.toList());

            // 3. 存入 MySQL (原材料入库)
            companyRepository.saveAll(companies);
            log.info("✅ 已成功入库 {} 家公司", companies.size());

            // 4. 🔥【核心联动】立即触发 AI 分析任务
            log.info("🚀 触发 DeepSeek 批量分析...");
            analysisTask.executeBatchAnalysis();

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("搜索并分析流程失败: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 💰 赚钱接口：客户给你 Excel/名单，你把 URL 列表传进来，系统自动出分析。
     * * 用法：
     * POST /crawler/linkedin/batch-analyze
     * Body: ["https://www.linkedin.com/company/stripe", "https://www.linkedin.com/company/adyen"]
     */
    @PostMapping("/linkedin/batch-analyze")
    public ResponseEntity<String> batchAnalyzeUrls(@RequestBody List<String> linkedinUrls) {
        log.info("💰 收到客户提供的名单，共 {} 个目标", linkedinUrls.size());

        // 1. 启动批量抓取 (LinkedInCrawlerTask 已经具备去重和入库功能)
        // 注意：这里调用的是 task 的 executeBatchCrawl，它会存库
        var crawlResult = linkedinCrawlerTask.executeBatchCrawl(linkedinUrls);

        log.info("✅ 名单抓取入库完成，成功: {}, 失败: {}。即将开始深度分析...",
                crawlResult.getSuccessCount(), crawlResult.getFailureCount());

        // 2. 立即触发 DeepSeek 分析 (分析刚才入库的那些)
        analysisTask.executeBatchAnalysis();

        return ResponseEntity.ok(String.format(
                "订单已接收！\n成功抓取: %d 家\n系统正在后台进行 DeepSeek 深度分析。\n请 2 分钟后访问 /api/export/pdf/latest 下载报告发给客户。",
                crawlResult.getSuccessCount()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Crawler service is healthy");
    }
}