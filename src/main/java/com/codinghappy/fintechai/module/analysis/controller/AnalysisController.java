package com.codinghappy.fintechai.module.analysis.controller;

import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.service.DeepSeekAnalysisService;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Validated
public class AnalysisController {

    private final DeepSeekAnalysisService analysisService;
    // private final RateLimitService rateLimitService; // 暂时注释

    /**
     * 分析单个公司
     */
    @PostMapping("/single")
    public ResponseEntity<AnalysisResultEntity> analyzeSingle(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("🔍 分析单个公司请求: {}", request.getCompanyName());

        try {
            // 调用 Service，直接获取 Entity 结果
            AnalysisResultEntity result = analysisService.analyzeCompany(
                    request.getCompanyId(),
                    request.getCompanyName(),
                    request.getDescription()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ 分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量分析公司
     */
    @PostMapping("/batch")
    public ResponseEntity<List<AnalysisResultEntity>> analyzeBatch(
            @Valid @RequestBody List<AnalysisRequest> requests) {
        log.info("🚀 批量分析请求，数量: {}", requests.size());

        try {
            List<AnalysisResultEntity> results = analysisService.batchAnalyze(requests);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("批量分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 清空分析缓存 (临时实现，防止报错)
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        log.info("清空分析缓存 (暂未实现具体逻辑)");
        // analysisService.clearCache();
        return ResponseEntity.ok().build();
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Analysis Service (DeepSeek V3 Commercial) is Ready.");
    }
}