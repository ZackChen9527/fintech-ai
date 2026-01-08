package com.codinghappy.fintechai.module.analysis.task;

import com.codinghappy.fintechai.module.analysis.service.DeepSeekAnalysisService;
import com.codinghappy.fintechai.repository.AnalysisResultRepository;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AnalysisTask {

    @Autowired
    private DeepSeekAnalysisService deepSeekAnalysisService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Value("${finance.analysis.task.batch-size:20}")
    private int batchSize;

    @Value("${finance.analysis.task.delay-seconds:2}")
    private int delaySeconds;

    @Value("${finance.analysis.task.enabled:true}")
    private boolean enabled;

    @Value("${finance.analysis.task.max-companies:100}")
    private int maxCompanies;

    @Value("${finance.analysis.task.score-threshold:8}")
    private int scoreThreshold;

    /**
     * ✅ 补回：执行单次公司分析 (Controller 需要调用这个)
     */
    public AnalysisResultEntity executeSingleAnalysis(Long companyId) {
        if (!enabled) {
            log.warn("分析任务已禁用");
            return null;
        }

        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        if (company == null || company.getDescription() == null) {
            log.warn("公司无效或描述为空 ID: {}", companyId);
            return null;
        }

        try {
            // 直接调用 Service
            log.info("手动触发单次分析: {}", company.getName());
            return deepSeekAnalysisService.analyzeCompany(
                    company.getId(),
                    company.getName(),
                    company.getDescription()
            );

        } catch (Exception e) {
            log.error("单次分析失败 ID: {}", companyId, e);
            return null;
        }
    }

    /**
     * 批量分析入口
     */
    @Transactional
    public void executeBatchAnalysis() {
        if (!enabled) {
            log.warn("分析任务已禁用");
            return;
        }

        log.info(">>> 开始执行批量分析任务...");
        List<CompanyEntity> unanalyzedCompanies = findUnanalyzedCompanies(maxCompanies);

        if (unanalyzedCompanies.isEmpty()) {
            log.info("没有待分析的公司");
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);

        for (CompanyEntity company : unanalyzedCompanies) {
            try {
                // 核心：调用 Service 进行分析
                AnalysisResultEntity result = deepSeekAnalysisService.analyzeCompany(
                        company.getId(),
                        company.getName(),
                        company.getDescription()
                );

                if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                    successCount.incrementAndGet();
                }

                // 礼貌性延迟
                if (delaySeconds > 0) TimeUnit.SECONDS.sleep(delaySeconds);

            } catch (Exception e) {
                log.error("分析异常: {}", company.getName(), e);
            }
        }
        log.info("<<< 批量任务结束，成功分析: {} 家", successCount.get());
    }

    /**
     * 获取任务状态 (ReportGenerationJob 必须调用这个)
     */
    public TaskStatus getTaskStatus() {
        long totalCompanies = companyRepository.count();
        long totalResults = analysisResultRepository.count();
        Double avgScore = analysisResultRepository.findAverageScore();
        Long highScoreCount = analysisResultRepository.countHighScoreResults();

        return new TaskStatus(
                enabled,
                batchSize,
                3, // maxRetries 固定
                delaySeconds,
                totalCompanies,
                totalResults,
                avgScore != null ? avgScore : 0.0,
                highScoreCount != null ? highScoreCount : 0L
        );
    }

    private List<CompanyEntity> findUnanalyzedCompanies(int limit) {
        List<Long> analyzedIds = analysisResultRepository.findAll()
                .stream().map(AnalysisResultEntity::getCompanyId).collect(Collectors.toList());

        return companyRepository.findAll(PageRequest.of(0, 500)).getContent().stream()
                .filter(c -> !analyzedIds.contains(c.getId()))
                .filter(c -> c.getDescription() != null && c.getDescription().length() > 5)
                .limit(limit)
                .collect(Collectors.toList());
    }

    // --- 内部类：TaskStatus ---
    @Data
    public static class TaskStatus {
        private final boolean enabled;
        private final int batchSize;
        private final int maxRetries;
        private final int delaySeconds;
        private final long totalCompanies;
        private final long totalAnalysisResults;
        private final double averageScore;
        private final long highScoreCount;
        private final LocalDateTime checkTime;
        private final double analysisCoverage;

        public TaskStatus(boolean enabled, int batchSize, int maxRetries, int delaySeconds,
                          long totalCompanies, long totalAnalysisResults,
                          double averageScore, long highScoreCount) {
            this.enabled = enabled;
            this.batchSize = batchSize;
            this.maxRetries = maxRetries;
            this.delaySeconds = delaySeconds;
            this.totalCompanies = totalCompanies;
            this.totalAnalysisResults = totalAnalysisResults;
            this.averageScore = averageScore;
            this.highScoreCount = highScoreCount;
            this.checkTime = LocalDateTime.now();
            this.analysisCoverage = totalCompanies > 0 ?
                    (double) totalAnalysisResults / totalCompanies * 100 : 0;
        }
    }
}