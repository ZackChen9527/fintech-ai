package com.codinghappy.fintechai.module.scheduler.job;

import com.codinghappy.fintechai.module.analysis.task.AnalysisTask;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ReportGenerationJob implements Job {

    @Autowired
    private AnalysisTask analysisTask;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行报告生成任务...");

        try {
            // 1. 获取任务状态
            var taskStatus = analysisTask.getTaskStatus();

            // 2. 生成报告内容
            String reportContent = generateReportContent(taskStatus);

            // 3. 保存报告到文件
            saveReportToFile(reportContent);

            // 4. 将报告内容保存到JobDataMap
            context.getJobDetail().getJobDataMap().put("reportGenerated", true);
            context.getJobDetail().getJobDataMap().put("reportTimestamp", LocalDateTime.now().toString());

            log.info("报告生成任务完成");

        } catch (Exception e) {
            log.error("报告生成任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }

    private String generateReportContent(AnalysisTask.TaskStatus taskStatus) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append("金融精准获客系统 - 分析报告\n");
        sb.append("生成时间: ").append(LocalDateTime.now().format(formatter)).append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append("1. 系统状态\n");
        sb.append("   任务状态: ").append(taskStatus.isEnabled() ? "运行中" : "已禁用").append("\n");
        sb.append("   批处理大小: ").append(taskStatus.getBatchSize()).append("\n");
        sb.append("   最大重试次数: ").append(taskStatus.getMaxRetries()).append("\n");
        sb.append("   延迟秒数: ").append(taskStatus.getDelaySeconds()).append("\n\n");

        sb.append("2. 数据分析\n");
        sb.append("   总公司数量: ").append(taskStatus.getTotalCompanies()).append("\n");
        sb.append("   分析结果数量: ").append(taskStatus.getTotalAnalysisResults()).append("\n");
        sb.append("   分析覆盖率: ").append(String.format("%.2f", taskStatus.getAnalysisCoverage())).append("%\n\n");

        sb.append("3. 评分统计\n");
        sb.append("   平均评分: ").append(String.format("%.2f", taskStatus.getAverageScore())).append("/10\n");
        sb.append("   高分客户数量(≥7分): ").append(taskStatus.getHighScoreCount()).append("\n");
        sb.append("   高分客户比例: ").append(String.format("%.2f",
                taskStatus.getTotalAnalysisResults() > 0 ?
                        (double) taskStatus.getHighScoreCount() / taskStatus.getTotalAnalysisResults() * 100 : 0
        )).append("%\n\n");

        sb.append("4. 任务执行\n");
        sb.append("   检查时间: ").append(taskStatus.getCheckTime().format(formatter)).append("\n");

        return sb.toString();
    }

    private void saveReportToFile(String reportContent) throws IOException {
        // 创建报告目录
        Path reportDir = Paths.get("logs/reports");
        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
        }

        // 生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("analysis_report_%s.txt", timestamp);
        Path filePath = reportDir.resolve(fileName);

        // 写入文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(reportContent);
        }

        log.info("报告已保存到: {}", filePath);
    }
}