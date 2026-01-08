package com.codinghappy.fintechai.module.analysis.controller;

import com.codinghappy.fintechai.module.analysis.task.AnalysisTask;
// 记得导入新实体
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/analysis/task")
@RequiredArgsConstructor
public class AnalysisTaskController {

    // ❌ 删除旧的 AnalysisService
    // private final AnalysisService analysisService;

    // ✅ 换成刚刚修好的 AnalysisTask
    private final AnalysisTask analysisTask;

    /**
     * 手动触发批量分析任务
     */
    @PostMapping("/batch/execute")
    public ResponseEntity<String> executeBatchTask() {
        log.info("收到手动触发批量分析指令...");

        // 异步或直接执行任务
        analysisTask.executeBatchAnalysis();

        // 获取当前状态作为反馈
        AnalysisTask.TaskStatus status = analysisTask.getTaskStatus();

        return ResponseEntity.ok(String.format(
                "✅ 批量分析任务已启动！当前系统累计已分析: %d 家，高价值线索: %d 家。",
                status.getTotalAnalysisResults(),
                status.getHighScoreCount()
        ));
    }

    /**
     * 手动触发单次分析任务 (通过公司ID)
     */
    @PostMapping("/single/execute/{companyId}")
    public ResponseEntity<?> executeSingleTask(@PathVariable Long companyId) {
        log.info("收到手动触发单次分析指令, 公司ID: {}", companyId);

        AnalysisResultEntity result = analysisTask.executeSingleAnalysis(companyId);

        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("分析失败：公司不存在或描述为空，或者任务被禁用。");
        }
    }

    /**
     * 获取任务系统状态看板
     */
    @GetMapping("/status")
    public ResponseEntity<AnalysisTask.TaskStatus> getTaskStatus() {
        return ResponseEntity.ok(analysisTask.getTaskStatus());
    }
}