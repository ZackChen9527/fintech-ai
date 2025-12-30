package com.codinghappy.fintechai.module.analysis.controller;


import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.service.DeepSeekAnalysisService;
import com.codinghappy.fintechai.module.analysis.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Validated
public class AnalysisController {

    private final DeepSeekAnalysisService analysisService;
    private final RateLimitService rateLimitService;

    /**
     * 分析单个公司
     */
    @PostMapping("/single")
    public ResponseEntity<AnalysisResult> analyzeSingle(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("分析单个公司: {}", request.getCompanyName());

        try {
            AnalysisResult result = analysisService.analyzeCompany(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(AnalysisResult.errorResult(e.getMessage()));
        }
    }

    /**
     * 批量分析公司
     */
    @PostMapping("/batch")
    public ResponseEntity<List<AnalysisResult>> analyzeBatch(
            @Valid @RequestBody List<AnalysisRequest> requests) {
        log.info("批量分析公司，数量: {}", requests.size());

        try {
            List<AnalysisResult> results = analysisService.batchAnalyze(requests);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("批量分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 清空分析缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        log.info("清空分析缓存");
        analysisService.clearCache();
        return ResponseEntity.ok().build();
    }

    /**
     * 获取限流状态
     */
    @GetMapping("/rate-limit/status")
    public ResponseEntity<RateLimitService.RateLimitStatus> getRateLimitStatus() {
        return ResponseEntity.ok(rateLimitService.getStatus());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analysis service is healthy");
    }
}