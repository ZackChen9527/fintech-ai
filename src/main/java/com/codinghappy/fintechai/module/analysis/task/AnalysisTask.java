package com.codinghappy.fintechai.module.analysis.task;


import com.codinghappy.fintechai.common.util.JsonUtil;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.service.DeepSeekAnalysisService;
import com.codinghappy.fintechai.repository.AnalysisResultRepository;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    @Value("${finance.analysis.task.max-retries:3}")
    private int maxRetries;

    @Value("${finance.analysis.task.delay-seconds:2}")
    private int delaySeconds;

    @Value("${finance.analysis.task.enabled:true}")
    private boolean enabled;

    @Value("${finance.analysis.task.max-companies:100}")
    private int maxCompanies;

    @Value("${finance.analysis.task.score-threshold:7}")
    private int scoreThreshold;

    /**
     * 执行单次公司分析任务
     */
    @Transactional
    public AnalysisResult executeSingleAnalysis(Long companyId) {
        if (!enabled) {
            log.warn("分析任务功能已禁用");
            return null;
        }

        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            log.error("公司不存在，ID: {}", companyId);
            return null;
        }

        if (company.getDescription() == null || company.getDescription().trim().isEmpty()) {
            log.warn("公司描述为空，跳过分析，公司ID: {}", companyId);
            return null;
        }

        log.info("开始分析公司，ID: {}, 名称: {}", companyId, company.getName());

        AnalysisRequest request = new AnalysisRequest();
        request.setCompanyName(company.getName());
        request.setDescription(company.getDescription());
        request.setWebsite(company.getWebsite());
        request.setLocation(company.getLocation());
        request.setCompanySize(company.getCompanySize());
        request.setIndustry(company.getIndustry());

        AnalysisResult result = null;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                // 执行分析
                result = deepSeekAnalysisService.analyzeCompany(request);

                if (result != null && result.isSuccess()) {
                    // 保存分析结果
                    saveAnalysisResult(companyId, result);
                    log.info("成功分析公司: {}，评分: {}",
                            company.getName(), result.getPaymentWillingnessScore());
                    break;
                } else {
                    log.warn("分析返回失败结果，公司ID: {}", companyId);
                    retryCount++;
                }

            } catch (Exception e) {
                retryCount++;
                log.error("分析失败 (重试 {}/{}), 公司ID: {}, 错误: {}",
                        retryCount, maxRetries, companyId, e.getMessage());

                if (retryCount <= maxRetries) {
                    try {
                        // 指数退避等待
                        long waitTime = (long) (delaySeconds * Math.pow(2, retryCount - 1));
                        log.info("等待 {} 秒后重试", waitTime);
                        TimeUnit.SECONDS.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("达到最大重试次数，放弃分析: {}", companyId);
                    saveFailedAnalysis(companyId, e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 批量分析未分析过的公司
     */
    @Transactional
    public AnalysisTaskResult executeBatchAnalysis() {
        if (!enabled) {
            log.warn("分析任务功能已禁用");
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("开始批量分析任务...");

        // 获取未分析过的公司
        List<CompanyEntity> unanalyzedCompanies = findUnanalyzedCompanies(maxCompanies);

        if (unanalyzedCompanies.isEmpty()) {
            log.info("没有需要分析的公司");
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("找到 {} 个未分析的公司", unanalyzedCompanies.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger highScoreCount = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);

        // 分批处理，避免内存溢出和API限流
        int totalBatches = (int) Math.ceil((double) unanalyzedCompanies.size() / batchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, unanalyzedCompanies.size());
            List<CompanyEntity> batchCompanies = unanalyzedCompanies.subList(fromIndex, toIndex);

            log.info("处理分析批次 {}/{}，数量: {}",
                    batchIndex + 1, totalBatches, batchCompanies.size());

            long batchStartTime = System.currentTimeMillis();

            batchCompanies.forEach(company -> {
                try {
                    long startTime = System.currentTimeMillis();

                    AnalysisResult result = executeSingleAnalysis(company.getId());

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    totalProcessingTime.addAndGet(processingTime);

                    if (result != null && result.isSuccess()) {
                        successCount.incrementAndGet();

                        // 统计高分公司
                        if (result.getPaymentWillingnessScore() != null &&
                                result.getPaymentWillingnessScore() >= scoreThreshold) {
                            highScoreCount.incrementAndGet();
                            log.info("发现高分潜在客户: {}，评分: {}",
                                    company.getName(), result.getPaymentWillingnessScore());
                        }

                        log.debug("公司分析完成: {}，耗时: {}ms",
                                company.getName(), processingTime);
                    } else {
                        failureCount.incrementAndGet();
                    }

                    // 批次内延迟，避免API限流
                    if (delaySeconds > 0) {
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("公司分析失败: {}, 错误: {}", company.getName(), e.getMessage());
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchProcessingTime = batchEndTime - batchStartTime;

            log.info("批次 {} 完成，耗时: {}ms，成功: {}，失败: {}",
                    batchIndex + 1, batchProcessingTime, successCount.get(), failureCount.get());

            // 批次间延迟
            if (batchIndex < totalBatches - 1 && delaySeconds > 0) {
                try {
                    TimeUnit.SECONDS.sleep(delaySeconds * 2L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        AnalysisTaskResult result = new AnalysisTaskResult(
                successCount.get(),
                failureCount.get(),
                highScoreCount.get(),
                totalProcessingTime.get()
        );

        log.info("批量分析任务完成: {}", result);

        // 生成分析报告
        generateAnalysisReport(result);

        return result;
    }

    /**
     * 重新分析低质量的分析结果
     */
    @Transactional
    public AnalysisTaskResult reanalyzeLowQualityResults() {
        if (!enabled) {
            log.warn("分析任务功能已禁用");
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("开始重新分析低质量分析结果...");

        // 查找低质量的分析结果（置信度低或评分异常）
        List<AnalysisResultEntity> lowQualityResults =
                analysisResultRepository.findByAnalysisTimeBetween(
                                LocalDateTime.now().minusDays(7),
                                LocalDateTime.now()
                        ).stream()
                        .filter(result ->
                                result.getConfidence() != null &&
                                        result.getConfidence() < 0.7
                        )
                        .limit(maxCompanies)
                        .collect(Collectors.toList());

        if (lowQualityResults.isEmpty()) {
            log.info("没有需要重新分析的低质量结果");
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("找到 {} 个低质量分析结果需要重新分析", lowQualityResults.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger improvedCount = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);

        lowQualityResults.forEach(oldResult -> {
            try {
                long startTime = System.currentTimeMillis();

                // 重新分析
                CompanyEntity company = companyRepository.findById(oldResult.getCompanyId()).orElse(null);
                if (company == null || company.getDescription() == null) {
                    failureCount.incrementAndGet();
                    return;
                }

                AnalysisRequest request = new AnalysisRequest();
                request.setCompanyName(company.getName());
                request.setDescription(company.getDescription());
                request.setWebsite(company.getWebsite());
                request.setLocation(company.getLocation());
                request.setCompanySize(company.getCompanySize());
                request.setIndustry(company.getIndustry());

                AnalysisResult newResult = deepSeekAnalysisService.analyzeCompany(request);

                long endTime = System.currentTimeMillis();
                totalProcessingTime.addAndGet(endTime - startTime);

                if (newResult != null && newResult.isSuccess()) {
                    successCount.incrementAndGet();

                    // 检查是否有改进
                    if (newResult.getConfidence() != null &&
                            oldResult.getConfidence() != null &&
                            newResult.getConfidence() > oldResult.getConfidence() + 0.1) {
                        improvedCount.incrementAndGet();
                    }

                    // 保存新的分析结果
                    saveAnalysisResult(oldResult.getCompanyId(), newResult);

                    log.info("重新分析完成，公司ID: {}，新置信度: {}",
                            oldResult.getCompanyId(), newResult.getConfidence());
                } else {
                    failureCount.incrementAndGet();
                }

                // 延迟
                if (delaySeconds > 0) {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                }

            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.error("重新分析失败，公司ID: {}", oldResult.getCompanyId(), e);
            }
        });

        AnalysisTaskResult result = new AnalysisTaskResult(
                successCount.get(),
                failureCount.get(),
                improvedCount.get(),
                totalProcessingTime.get()
        );

        log.info("重新分析任务完成: {}", result);

        return result;
    }

    /**
     * 异步执行批量分析
     */
    @Async("analysisThreadPool")
    public CompletableFuture<AnalysisTaskResult> executeAsyncBatchAnalysis() {
        log.info("开始异步批量分析任务...");

        AnalysisTaskResult result = executeBatchAnalysis();

        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分析指定行业的公司
     */
    public AnalysisTaskResult analyzeByIndustry(String industry, int limit) {
        if (!enabled) {
            log.warn("分析任务功能已禁用");
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("分析指定行业的公司，行业: {}，限制: {}", industry, limit);

        List<CompanyEntity> companies = companyRepository.findByIndustry(industry)
                .stream()
                .limit(Math.min(limit, maxCompanies))
                .collect(Collectors.toList());

        if (companies.isEmpty()) {
            log.info("没有找到指定行业的公司: {}", industry);
            return new AnalysisTaskResult(0, 0, 0, 0);
        }

        log.info("找到 {} 个 {} 行业的公司", companies.size(), industry);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger highScoreCount = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);

        companies.forEach(company -> {
            try {
                long startTime = System.currentTimeMillis();

                AnalysisResult result = executeSingleAnalysis(company.getId());

                long endTime = System.currentTimeMillis();
                totalProcessingTime.addAndGet(endTime - startTime);

                if (result != null && result.isSuccess()) {
                    successCount.incrementAndGet();

                    if (result.getPaymentWillingnessScore() != null &&
                            result.getPaymentWillingnessScore() >= scoreThreshold) {
                        highScoreCount.incrementAndGet();
                    }
                } else {
                    failureCount.incrementAndGet();
                }

                // 延迟
                if (delaySeconds > 0) {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                }

            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.error("行业分析失败: {}, 公司: {}", industry, company.getName(), e);
            }
        });

        AnalysisTaskResult result = new AnalysisTaskResult(
                successCount.get(),
                failureCount.get(),
                highScoreCount.get(),
                totalProcessingTime.get()
        );

        log.info("行业分析完成，行业: {}，结果: {}", industry, result);

        return result;
    }

    /**
     * 查找未分析过的公司
     */
    private List<CompanyEntity> findUnanalyzedCompanies(int limit) {
        // 获取所有公司
        List<CompanyEntity> allCompanies = companyRepository.findAll(
                PageRequest.of(0, Math.min(limit * 2, 1000))
        ).getContent();

        // 获取已有分析结果的公司ID
        List<Long> analyzedCompanyIds = analysisResultRepository.findAll()
                .stream()
                .map(AnalysisResultEntity::getCompanyId)
                .distinct()
                .collect(Collectors.toList());

        // 过滤出未分析的公司
        return allCompanies.stream()
                .filter(company ->
                        company.getIsActive() != null &&
                                company.getIsActive() &&
                                company.getDescription() != null &&
                                !company.getDescription().trim().isEmpty() &&
                                !analyzedCompanyIds.contains(company.getId())
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 保存分析结果到数据库
     */
    private void saveAnalysisResult(Long companyId, AnalysisResult result) {
        try {
            AnalysisResultEntity entity = new AnalysisResultEntity();
            entity.setCompanyId(companyId);

            // 保存业务类型
            if (result.getBusinessTypes() != null && !result.getBusinessTypes().isEmpty()) {
                String businessTypes = result.getBusinessTypes().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(","));
                entity.setBusinessTypes(businessTypes);
            }

            entity.setPaymentWillingnessScore(result.getPaymentWillingnessScore());
            entity.setConfidence(result.getConfidence());
            entity.setAnalysisReason(result.getAnalysisReason());
            entity.setRawResponse(result.getRawResponse());
            entity.setSuccess(result.isSuccess());
            entity.setAnalysisModel("DeepSeek");
            entity.setProcessingTimeMs(0.0); // 可以计算实际处理时间

            analysisResultRepository.save(entity);

            log.debug("分析结果保存成功，公司ID: {}", companyId);

        } catch (Exception e) {
            log.error("保存分析结果失败，公司ID: {}", companyId, e);
        }
    }

    /**
     * 保存失败的分析记录
     */
    private void saveFailedAnalysis(Long companyId, String errorMessage) {
        try {
            AnalysisResultEntity entity = new AnalysisResultEntity();
            entity.setCompanyId(companyId);
            entity.setSuccess(false);
            entity.setErrorMessage(errorMessage);
            entity.setAnalysisTime(LocalDateTime.now());

            analysisResultRepository.save(entity);

        } catch (Exception e) {
            log.error("保存失败记录出错: {}", e.getMessage());
        }
    }

    /**
     * 生成分析报告
     */
    private void generateAnalysisReport(AnalysisTaskResult result) {
        try {
            // 计算平均评分
            Double averageScore = analysisResultRepository.findAverageScore();

            // 统计高分客户数量
            Long highScoreCount = analysisResultRepository.countHighScoreResults();

            // 生成报告
            AnalysisReport report = new AnalysisReport();
            report.setReportDate(LocalDateTime.now());
            report.setTotalAnalyzed(result.getTotalProcessed());
            report.setSuccessCount(result.getSuccessCount());
            report.setFailureCount(result.getFailureCount());
            report.setHighScoreCount(highScoreCount != null ? highScoreCount : 0L);
            report.setAverageScore(averageScore != null ? averageScore : 0.0);
            report.setSuccessRate(result.getSuccessRate());
            report.setProcessingTimeMs(result.getTotalProcessingTime());

            log.info("分析报告生成完成: {}", report.toJson());

            // 这里可以添加发送报告的逻辑，如发送邮件、保存到数据库等

        } catch (Exception e) {
            log.error("生成分析报告失败", e);
        }
    }

    /**
     * 获取任务状态
     */
    public TaskStatus getTaskStatus() {
        long totalCompanies = companyRepository.count();
        long totalAnalysisResults = analysisResultRepository.count();
        Double averageScore = analysisResultRepository.findAverageScore();
        Long highScoreCount = analysisResultRepository.countHighScoreResults();

        return new TaskStatus(
                enabled,
                batchSize,
                maxRetries,
                delaySeconds,
                totalCompanies,
                totalAnalysisResults,
                averageScore != null ? averageScore : 0.0,
                highScoreCount != null ? highScoreCount : 0L
        );
    }

    /**
     * 清除旧的分析结果
     */
    @Transactional
    public int cleanupOldResults(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);

            // 查找需要删除的结果
            List<AnalysisResultEntity> oldResults =
                    analysisResultRepository.findByAnalysisTimeBetween(
                            LocalDateTime.MIN,
                            cutoffTime
                    );

            int deletedCount = oldResults.size();

            // 删除旧结果
            analysisResultRepository.deleteAll(oldResults);

            log.info("清理了 {} 个 {} 天前的分析结果", deletedCount, daysToKeep);

            return deletedCount;

        } catch (Exception e) {
            log.error("清理旧结果失败", e);
            return 0;
        }
    }

    /**
     * 分析任务结果类
     */
    public static class AnalysisTaskResult {
        private final int successCount;
        private final int failureCount;
        private final int highScoreCount;
        private final long totalProcessingTime;
        private final LocalDateTime completionTime;

        public AnalysisTaskResult(int successCount, int failureCount,
                                  int highScoreCount, long totalProcessingTime) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.highScoreCount = highScoreCount;
            this.totalProcessingTime = totalProcessingTime;
            this.completionTime = LocalDateTime.now();
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getHighScoreCount() {
            return highScoreCount;
        }

        public long getTotalProcessingTime() {
            return totalProcessingTime;
        }

        public LocalDateTime getCompletionTime() {
            return completionTime;
        }

        public int getTotalProcessed() {
            return successCount + failureCount;
        }

        public double getSuccessRate() {
            int total = getTotalProcessed();
            return total > 0 ? (double) successCount / total * 100 : 0;
        }

        public double getAverageProcessingTime() {
            int total = getTotalProcessed();
            return total > 0 ? (double) totalProcessingTime / total : 0;
        }

        @Override
        public String toString() {
            return String.format("成功: %d, 失败: %d, 高分: %d, 成功率: %.2f%%, 平均耗时: %.2fms",
                    successCount, failureCount, highScoreCount, getSuccessRate(), getAverageProcessingTime());
        }

        public String toJson() {
            return JsonUtil.toJson(this);
        }
    }

    /**
     * 分析报告类
     */
    public static class AnalysisReport {
        private LocalDateTime reportDate;
        private int totalAnalyzed;
        private int successCount;
        private int failureCount;
        private long highScoreCount;
        private double averageScore;
        private double successRate;
        private long processingTimeMs;

        // Getters and Setters
        public LocalDateTime getReportDate() { return reportDate; }
        public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }

        public int getTotalAnalyzed() { return totalAnalyzed; }
        public void setTotalAnalyzed(int totalAnalyzed) { this.totalAnalyzed = totalAnalyzed; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

        public long getHighScoreCount() { return highScoreCount; }
        public void setHighScoreCount(long highScoreCount) { this.highScoreCount = highScoreCount; }

        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        public String toJson() {
            return JsonUtil.toJson(this);
        }
    }

    /**
     * 任务状态类
     */
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

        public TaskStatus(boolean enabled, int batchSize, int maxRetries,
                          int delaySeconds, long totalCompanies, long totalAnalysisResults,
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
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public int getBatchSize() { return batchSize; }
        public int getMaxRetries() { return maxRetries; }
        public int getDelaySeconds() { return delaySeconds; }
        public long getTotalCompanies() { return totalCompanies; }
        public long getTotalAnalysisResults() { return totalAnalysisResults; }
        public double getAverageScore() { return averageScore; }
        public long getHighScoreCount() { return highScoreCount; }
        public LocalDateTime getCheckTime() { return checkTime; }

        public double getAnalysisCoverage() {
            return totalCompanies > 0 ? (double) totalAnalysisResults / totalCompanies * 100 : 0;
        }

        public String toJson() {
            return JsonUtil.toJson(this);
        }
    }
}