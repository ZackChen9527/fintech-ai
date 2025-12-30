package com.codinghappy.fintechai.module.analysis.controller;

import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.service.AnalysisService;

import com.codinghappy.fintechai.repository.AnalysisResultRepository;
import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/analysis/task")
@RequiredArgsConstructor
public class AnalysisTaskController {

    private final CompanyRepository companyRepository;
    private final AnalysisService analysisService;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 自动化批量分析任务：从数据库读取公司数据，调用AI分析，并将结果存入数据库
     * 对应 APIfox 中的 POST /api/analysis/task/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<String> runBatchAnalysisTask() {
        log.info("启动自动化批量分析任务...");

        // 1. 从数据库捞取所有需要分析的公司
        List<CompanyEntity> companies = companyRepository.findAll();

        if (companies.isEmpty()) {
            return ResponseEntity.ok("数据库中暂无公司数据，请先执行抓取任务获取线索");
        }

        int successCount = 0;
        for (CompanyEntity company : companies) {
            try {
                // 2. 物理降频：强制休息 1.5 秒，确保完全避开本地限流和 AI 频率限制
                Thread.sleep(1500);

                log.info("正在调遣 AI 深度分析公司: {}", company.getName());

                // 3. 构造分析请求 DTO
                AnalysisRequest request = new AnalysisRequest();
                request.setCompanyName(company.getName());
                request.setDescription(company.getDescription());

                // 4. 调用已有的 AI 分析服务 (获取 DTO 结果)
                AnalysisResult result = analysisService.analyzeCompany(request);

                // 5. 核心逻辑：将分析结果 DTO 转换为数据库实体 Entity 并持久化
                if (result != null && result.isSuccess()) {
                    AnalysisResultEntity entity = new AnalysisResultEntity();

                    // 建立外键关联
                    entity.setCompanyId(company.getId());

                    // 填充 AI 分析产出的黄金数据
                    entity.setPaymentWillingnessScore(result.getPaymentWillingnessScore());
                    entity.setConfidence(result.getConfidence());
                    entity.setAnalysisReason(result.getAnalysisReason());

                    // 处理业务类型：将 List 转换为逗号分隔的字符串存储
                    if (result.getBusinessTypes() != null) {
                        String types = result.getBusinessTypes().stream()
                                .map(Enum::name)
                                .collect(Collectors.joining(","));
                        entity.setBusinessTypes(types);
                    }

                    entity.setAnalysisTime(LocalDateTime.now());
                    entity.setSuccess(true);

                    // 6. 执行真正的数据库保存操作
                    analysisResultRepository.save(entity);
                    successCount++;
                    log.info("成功！公司 {} 的分析报告已入库。AI 评分: {}",
                            company.getName(), result.getPaymentWillingnessScore());
                }

            } catch (InterruptedException e) {
                log.error("分析任务被意外中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("分析公司 {} 失败，原因: {}", company.getName(), e.getMessage());
            }
        }

        return ResponseEntity.ok("全自动批量分析任务已完成！本次成功产出高价值线索: " + successCount + " 条");
    }
}