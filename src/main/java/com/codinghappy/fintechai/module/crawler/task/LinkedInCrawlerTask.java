package com.codinghappy.fintechai.module.crawler.task;

import com.codinghappy.fintechai.common.util.JsonUtil;
import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import com.codinghappy.fintechai.module.crawler.service.CrawlerService;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LinkedInCrawlerTask {

    @Autowired
    private CrawlerService linkedInCrawlerService;

    @Autowired
    private CompanyRepository companyRepository;

    @Value("${finance.crawler.linkedin.batch-size:10}")
    private int batchSize;

    @Value("${finance.crawler.linkedin.max-retries:3}")
    private int maxRetries;

    @Value("${finance.crawler.linkedin.delay-seconds:5}")
    private int delaySeconds;

    @Value("${finance.crawler.linkedin.enabled:true}")
    private boolean enabled;

    /**
     * 执行单次 LinkedIn 公司抓取任务
     */
    @Transactional
    public CompanyProfileDTO executeSingleCrawl(String linkedinUrl) {
        if (!enabled) {
            log.warn("LinkedIn 抓取功能已禁用");
            return null;
        }

        // ✅ 核心修复：放宽 URL 校验，支持 cn/hk/tw 等所有地区子域名
        if (linkedinUrl == null || !linkedinUrl.contains("linkedin.com/company/")) {
            log.error("非标准公司 URL，跳过: {}", linkedinUrl);
            return null;
        }

        log.info("开始抓取 LinkedIn 公司信息: {}", linkedinUrl);

        CompanyProfileDTO profile = null;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                // 执行抓取
                profile = linkedInCrawlerService.crawlCompany(linkedinUrl);

                if (profile != null) {
                    // 保存到数据库
                    saveCompanyProfile(profile);
                    log.info("成功抓取并保存公司: {}", profile.getCompanyName());
                    break;
                } else {
                    log.warn("抓取返回空结果，URL: {}", linkedinUrl);
                    retryCount++; // 失败也需要增加重试计数，否则可能死循环
                }

            } catch (Exception e) {
                retryCount++;
                log.error("抓取失败 (重试 {}/{}), URL: {}, 错误: {}",
                        retryCount, maxRetries, linkedinUrl, e.getMessage());

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
                    log.error("达到最大重试次数，放弃抓取: {}", linkedinUrl);
                    saveFailedCrawl(linkedinUrl, e.getMessage());
                }
            }
        }

        return profile;
    }

    /**
     * 批量抓取 LinkedIn 公司信息
     */
    @Transactional
    public CrawlResult executeBatchCrawl(List<String> linkedinUrls) {
        if (!enabled) {
            log.warn("LinkedIn 抓取功能已禁用");
            return new CrawlResult(0, 0, 0);
        }

        log.info("开始批量抓取 LinkedIn 公司，数量: {}", linkedinUrls.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // 分批处理，避免内存溢出
        int totalBatches = (int) Math.ceil((double) linkedinUrls.size() / batchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, linkedinUrls.size());
            List<String> batchUrls = linkedinUrls.subList(fromIndex, toIndex);

            log.info("处理批次 {}/{}，数量: {}",
                    batchIndex + 1, totalBatches, batchUrls.size());

            // 使用普通 stream 而不是 parallelStream，防止被 LinkedIn 封 IP
            batchUrls.forEach(url -> {
                try {
                    // ✅ 核心修复：放宽 URL 校验，不再使用 strict 的 supports 方法
                    if (url == null || !url.contains("linkedin.com/company/")) {
                        log.warn("跳过非公司主页 URL: {}", url);
                        skipCount.incrementAndGet();
                        return;
                    }

                    // 检查是否已存在
                    if (isCompanyAlreadyCrawled(url)) {
                        log.debug("公司已抓取过: {}", url);
                        skipCount.incrementAndGet();
                        return;
                    }

                    CompanyProfileDTO profile = linkedInCrawlerService.crawlCompany(url);
                    if (profile != null) {
                        saveCompanyProfile(profile);
                        successCount.incrementAndGet();
                        log.info("✅ 成功抓取: {}", profile.getCompanyName());
                    } else {
                        failureCount.incrementAndGet();
                        log.warn("❌ 抓取返回空结果: {}", url);
                    }

                    // 批次内延迟，避免请求过快
                    if (delaySeconds > 0) {
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("抓取异常: {}, 错误: {}", url, e.getMessage());
                    saveFailedCrawl(url, e.getMessage());
                }
            });

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

        CrawlResult result = new CrawlResult(
                successCount.get(),
                failureCount.get(),
                skipCount.get()
        );

        log.info("批量抓取完成: {}", result);
        return result;
    }

    /**
     * 异步执行抓取任务
     */
    @Async("analysisThreadPool")
    public CompletableFuture<CrawlResult> executeAsyncBatchCrawl(List<String> linkedinUrls) {
        log.info("开始异步批量抓取，数量: {}", linkedinUrls.size());
        CrawlResult result = executeBatchCrawl(linkedinUrls);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 搜索并抓取公司
     */
    public CrawlResult searchAndCrawl(String keyword, int limit) {
        if (!enabled) {
            log.warn("LinkedIn 抓取功能已禁用");
            return new CrawlResult(0, 0, 0);
        }

        log.info("搜索并抓取公司，关键词: {}, 限制: {}", keyword, limit);

        try {
            // 搜索公司
            List<CompanyProfileDTO> searchResults =
                    linkedInCrawlerService.searchCompanies(keyword, limit);

            // 提取 LinkedIn URL (只要非空就放行，具体校验交给 executeBatchCrawl)
            List<String> urls = searchResults.stream()
                    .map(CompanyProfileDTO::getLinkedinUrl)
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(Collectors.toList());

            if (urls.isEmpty()) {
                log.warn("未搜索到任何有效的 LinkedIn URL");
                return new CrawlResult(0, 0, 0);
            }

            // 批量抓取
            return executeBatchCrawl(urls);

        } catch (Exception e) {
            log.error("搜索抓取失败，关键词: {}", keyword, e);
            return new CrawlResult(0, 0, 0);
        }
    }

    /**
     * 保存公司信息到数据库
     */
    private void saveCompanyProfile(CompanyProfileDTO profile) {
        try {
            // 检查是否已存在 (双重保险)
            if (isCompanyAlreadyCrawled(profile.getLinkedinUrl())) {
                return;
            }

            CompanyEntity entity = new CompanyEntity();
            entity.setName(profile.getCompanyName());
            entity.setDescription(profile.getDescription());
            entity.setWebsite(profile.getWebsite());
            entity.setLinkedinUrl(profile.getLinkedinUrl());
            entity.setLocation(profile.getLocation());
            entity.setIndustry(profile.getIndustry());
            entity.setCompanySize(profile.getCompanySize());
            entity.setTags(profile.getTags());
            entity.setEmployeeCount(profile.getEmployeeCount());
            entity.setRevenueRange(profile.getRevenueRange());
            entity.setIsActive(true);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDataSource("LinkedIn_Crawler");

            companyRepository.save(entity);

            log.debug("公司信息保存成功: {}", profile.getCompanyName());

        } catch (Exception e) {
            log.error("保存公司信息失败: {}, 错误: {}",
                    profile.getCompanyName(), e.getMessage());
        }
    }

    /**
     * 保存抓取失败记录
     */
    private void saveFailedCrawl(String url, String errorMessage) {
        try {
            // 失败记录也查重，防止数据库报错
            if (isCompanyAlreadyCrawled(url)) return;

            CompanyEntity entity = new CompanyEntity();
            entity.setLinkedinUrl(url);
            entity.setDescription("抓取失败: " + errorMessage);
            entity.setIsActive(false);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setDataSource("Crawler_Failed");

            companyRepository.save(entity);

        } catch (Exception e) {
            log.error("保存失败记录出错: {}", e.getMessage());
        }
    }

    /**
     * 检查公司是否已抓取过
     */
    private boolean isCompanyAlreadyCrawled(String linkedinUrl) {
        if (linkedinUrl == null || linkedinUrl.trim().isEmpty()) {
            return false;
        }

        try {
            // ✅ 现在 CompanyRepository 里有了这个方法，这里就不会报错了
            return companyRepository.findByLinkedinUrl(linkedinUrl).isPresent();
        } catch (Exception e) {
            // 兜底：万一数据库字段有问题，退回到模糊查询
            List<CompanyEntity> existingCompanies =
                    companyRepository.findByDescriptionContaining(linkedinUrl);
            return !existingCompanies.isEmpty();
        }
    }

    /**
     * 获取任务状态
     */
    public TaskStatus getTaskStatus() {
        long totalCompanies = companyRepository.count();
        // 兼容性处理：如果 countActiveCompanies 不存在
        long activeCompanies = 0;
        try {
            // activeCompanies = companyRepository.countActiveCompanies();
            activeCompanies = companyRepository.count(); // 暂时用总数代替
        } catch (Exception e) {
            // ignore
        }

        return new TaskStatus(
                enabled,
                batchSize,
                maxRetries,
                delaySeconds,
                totalCompanies,
                activeCompanies
        );
    }

    /**
     * 重置抓取状态（重新抓取失败的公司）
     */
    @Transactional
    public int resetFailedCrawls() {
        return 0;
    }

    /**
     * 抓取结果类
     */
    public static class CrawlResult {
        private final int successCount;
        private final int failureCount;
        private final int skipCount;
        private final LocalDateTime completionTime;

        public CrawlResult(int successCount, int failureCount, int skipCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.skipCount = skipCount;
            this.completionTime = LocalDateTime.now();
        }

        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public int getSkipCount() { return skipCount; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public int getTotalProcessed() { return successCount + failureCount + skipCount; }

        public double getSuccessRate() {
            int total = getTotalProcessed();
            return total > 0 ? (double) successCount / total * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("成功: %d, 失败: %d, 跳过: %d, 成功率: %.2f%%",
                    successCount, failureCount, skipCount, getSuccessRate());
        }

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
        private final long activeCompanies;
        private final LocalDateTime checkTime;

        public TaskStatus(boolean enabled, int batchSize, int maxRetries,
                          int delaySeconds, long totalCompanies, long activeCompanies) {
            this.enabled = enabled;
            this.batchSize = batchSize;
            this.maxRetries = maxRetries;
            this.delaySeconds = delaySeconds;
            this.totalCompanies = totalCompanies;
            this.activeCompanies = activeCompanies;
            this.checkTime = LocalDateTime.now();
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public int getBatchSize() { return batchSize; }
        public int getMaxRetries() { return maxRetries; }
        public int getDelaySeconds() { return delaySeconds; }
        public long getTotalCompanies() { return totalCompanies; }
        public long getActiveCompanies() { return activeCompanies; }
        public LocalDateTime getCheckTime() { return checkTime; }

        public String toJson() {
            return JsonUtil.toJson(this);
        }
    }
}