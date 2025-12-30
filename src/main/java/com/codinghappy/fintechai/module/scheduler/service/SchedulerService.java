package com.codinghappy.fintechai.module.scheduler.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

@Slf4j
@Service
public class SchedulerService {

    private final Scheduler scheduler;

    public SchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() throws SchedulerException {
        if (!scheduler.isStarted()) {
            scheduler.start();
            log.info("任务调度器已启动");
        }
    }

    @PreDestroy
    public void destroy() throws SchedulerException {
        if (scheduler.isStarted()) {
            scheduler.shutdown(true);
            log.info("任务调度器已关闭");
        }
    }

    /**
     * 添加一次性任务
     */
    public Date scheduleOneTimeJob(JobDetail jobDetail, Trigger trigger)
            throws SchedulerException {
        return scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 添加Cron任务
     */
    public Date scheduleCronJob(JobDetail jobDetail, String cronExpression)
            throws SchedulerException {
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName() + "-trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        return scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 暂停任务
     */
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        scheduler.pauseJob(jobKey);
        log.info("任务已暂停: {}", jobKey);
    }

    /**
     * 恢复任务
     */
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        scheduler.resumeJob(jobKey);
        log.info("任务已恢复: {}", jobKey);
    }

    /**
     * 删除任务
     */
    public boolean deleteJob(JobKey jobKey) throws SchedulerException {
        boolean result = scheduler.deleteJob(jobKey);
        if (result) {
            log.info("任务已删除: {}", jobKey);
        }
        return result;
    }

    /**
     * 立即执行任务
     */
    public void triggerJob(JobKey jobKey) throws SchedulerException {
        scheduler.triggerJob(jobKey);
        log.info("立即执行任务: {}", jobKey);
    }

    /**
     * 检查任务是否存在
     */
    public boolean checkJobExists(JobKey jobKey) throws SchedulerException {
        return scheduler.checkExists(jobKey);
    }

    /**
     * 获取任务状态
     */
    public Trigger.TriggerState getJobState(JobKey jobKey) throws SchedulerException {
        return scheduler.getTriggerState(
                TriggerKey.triggerKey(jobKey.getName() + "-trigger")
        );
    }

    /**
     * 获取所有任务
     */
    public Set<JobKey> getAllJobs() throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.anyGroup());
    }
}