package com.codinghappy.fintechai.module.scheduler.job;


import com.codinghappy.fintechai.module.crawler.task.LinkedInCrawlerTask;
import com.codinghappy.fintechai.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LeadGenerationJob implements Job {

    @Autowired
    private LinkedInCrawlerTask linkedInCrawlerTask;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行潜在客户生成任务...");

        try {
            // 1. 获取任务状态
            var taskStatus = linkedInCrawlerTask.getTaskStatus();
            log.info("LinkedIn抓取任务状态: {}", taskStatus.toJson());

            // 2. 执行抓取任务（示例：搜索金融科技相关公司）
            var result = linkedInCrawlerTask.searchAndCrawl("金融科技", 10);

            // 3. 统计结果
            long totalCompanies = companyRepository.count();
            long activeCompanies = companyRepository.countActiveCompanies();

            // 4. 保存结果到JobDataMap
            context.getJobDetail().getJobDataMap().put("crawlResult", result.toJson());
            context.getJobDetail().getJobDataMap().put("companyStats",
                    String.format("总数: %d, 活跃: %d", totalCompanies, activeCompanies));

            log.info("潜在客户生成任务完成: {}", result);

        } catch (Exception e) {
            log.error("潜在客户生成任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}