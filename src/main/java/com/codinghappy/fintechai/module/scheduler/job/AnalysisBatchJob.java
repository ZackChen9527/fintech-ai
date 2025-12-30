package com.codinghappy.fintechai.module.scheduler.job;


import com.codinghappy.fintechai.module.analysis.task.AnalysisTask;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisBatchJob implements Job {

    @Autowired
    private AnalysisTask analysisTask;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行批量分析任务...");

        try {
            // 1. 获取任务状态
            var taskStatus = analysisTask.getTaskStatus();
            log.info("分析任务状态: {}", taskStatus.toJson());

            // 2. 执行批量分析
            var result = analysisTask.executeBatchAnalysis();

            // 3. 保存结果到JobDataMap
            context.getJobDetail().getJobDataMap().put("analysisResult", result.toJson());
            context.getJobDetail().getJobDataMap().put("highScoreCount", result.getHighScoreCount());

            log.info("批量分析任务完成: {}", result);

        } catch (Exception e) {
            log.error("批量分析任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}