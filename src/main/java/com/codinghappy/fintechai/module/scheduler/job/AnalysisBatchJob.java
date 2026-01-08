package com.codinghappy.fintechai.module.scheduler.job;

import com.alibaba.fastjson.JSON; // 引入 Fastjson
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
        log.info(">>> 开始执行批量分析定时任务...");

        try {
            // 1. 记录开始前的状态 (可选)
            // AnalysisTask.TaskStatus preStatus = analysisTask.getTaskStatus();
            // log.info("任务前状态: {}", JSON.toJSONString(preStatus));

            // 2. 执行批量分析 (注意：现在是 void，不返回结果)
            analysisTask.executeBatchAnalysis();

            // 3. 获取执行后的最新状态 (用来替代原来的 result)
            AnalysisTask.TaskStatus postStatus = analysisTask.getTaskStatus();

            // 4. 保存关键指标到 JobDataMap
            context.getJobDetail().getJobDataMap().put("currentTotalAnalyzed", postStatus.getTotalAnalysisResults());
            context.getJobDetail().getJobDataMap().put("highScoreCount", postStatus.getHighScoreCount());

            // 使用 JSON.toJSONString 来序列化状态
            context.getJobDetail().getJobDataMap().put("taskStatusJson", JSON.toJSONString(postStatus));

            log.info("✅ 批量分析任务完成。当前系统累计分析: {} 家，高价值线索: {} 家",
                    postStatus.getTotalAnalysisResults(),
                    postStatus.getHighScoreCount());

        } catch (Exception e) {
            log.error("❌ 批量分析任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}