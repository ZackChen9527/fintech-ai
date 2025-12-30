package com.codinghappy.fintechai.module.scheduler.config;

import com.codinghappy.fintechai.module.scheduler.job.AnalysisBatchJob;
import com.codinghappy.fintechai.module.scheduler.job.CompanySyncJob;
import com.codinghappy.fintechai.module.scheduler.job.LeadGenerationJob;
import com.codinghappy.fintechai.module.scheduler.job.ReportGenerationJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    /**
     * 潜在客户生成任务
     */
    @Bean
    public JobDetail leadGenerationJobDetail() {
        return JobBuilder.newJob(LeadGenerationJob.class)
                .withIdentity("leadGenerationJob")
                .withDescription("定时生成潜在客户任务")
                .storeDurably()
                .build();
    }

    /**
     * 批量分析任务
     */
    @Bean
    public JobDetail analysisBatchJobDetail() {
        return JobBuilder.newJob(AnalysisBatchJob.class)
                .withIdentity("analysisBatchJob")
                .withDescription("批量分析任务")
                .storeDurably()
                .build();
    }

    /**
     * 潜在客户生成任务触发器
     */
    @Bean
    public Trigger leadGenerationJobTrigger(JobDetail leadGenerationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(leadGenerationJobDetail)
                .withIdentity("leadGenerationTrigger")
                .withDescription("每天凌晨2点触发")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
                .build();
    }

    /**
     * 批量分析任务触发器
     */
    @Bean
    public Trigger analysisBatchJobTrigger(JobDetail analysisBatchJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(analysisBatchJobDetail)
                .withIdentity("analysisBatchTrigger")
                .withDescription("每30分钟触发")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 */30 * * * ?"))
                .build();
    }

    /**
     * 公司数据同步任务（可选，如果您需要）
     */
    @Bean
    public JobDetail companySyncJobDetail() {
        return JobBuilder.newJob(CompanySyncJob.class)
                .withIdentity("companySyncJob")
                .withDescription("公司数据同步任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger companySyncJobTrigger(JobDetail companySyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(companySyncJobDetail)
                .withIdentity("companySyncTrigger")
                .withDescription("每小时同步一次公司数据")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }

    /**
     * 报告生成任务（可选）
     */
    @Bean
    public JobDetail reportGenerationJobDetail() {
        return JobBuilder.newJob(ReportGenerationJob.class)
                .withIdentity("reportGenerationJob")
                .withDescription("报告生成任务")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportGenerationJobTrigger(JobDetail reportGenerationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportGenerationJobDetail)
                .withIdentity("reportGenerationTrigger")
                .withDescription("每天凌晨1点生成报告")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 * * ?"))
                .build();
    }
}