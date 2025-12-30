package com.codinghappy.fintechai.module.scheduler.job;

import com.codinghappy.fintechai.module.analysis.task.AnalysisTask;
import com.codinghappy.fintechai.module.crawler.task.LinkedInCrawlerTask;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class CompanySyncJob implements Job {

    @Autowired
    private LinkedInCrawlerTask linkedInCrawlerTask;

    @Autowired
    private AnalysisTask analysisTask;

    @Value("${finance.crawler.linkedin.schedule.keywords:跨境支付,金融科技}")
    private String keywords;

    @Value("${finance.crawler.linkedin.schedule.limit-per-keyword:5}")
    private int limitPerKeyword;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行公司数据同步任务...");

        try {
            // 1. 抓取公司数据
            List<String> keywordList = Arrays.asList(keywords.split(","));
            int totalSuccess = 0;
            int totalFailure = 0;

            for (String keyword : keywordList) {
                log.info("搜索并抓取关键词: {}", keyword);

                var result = linkedInCrawlerTask.searchAndCrawl(keyword, limitPerKeyword);

                totalSuccess += result.getSuccessCount();
                totalFailure += result.getFailureCount();

                log.info("关键词 {} 抓取完成: {}", keyword, result);

                // 短暂延迟，避免请求过快
                Thread.sleep(5000);
            }

            log.info("公司数据抓取完成，总计成功: {}，失败: {}", totalSuccess, totalFailure);

            // 2. 分析新抓取的公司（如果有）
            if (totalSuccess > 0) {
                log.info("开始分析新抓取的公司...");
                var analysisResult = analysisTask.executeBatchAnalysis();
                log.info("新公司分析完成: {}", analysisResult);

                // 将结果保存到JobDataMap
                context.getJobDetail().getJobDataMap().put("syncResult",
                        String.format("抓取: %d成功/%d失败, 分析: %d成功/%d失败",
                                totalSuccess, totalFailure,
                                analysisResult.getSuccessCount(), analysisResult.getFailureCount()));
            }

        } catch (Exception e) {
            log.error("公司数据同步任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}