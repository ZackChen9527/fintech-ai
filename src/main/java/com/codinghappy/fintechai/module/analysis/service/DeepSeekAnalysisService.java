package com.codinghappy.fintechai.module.analysis.service;

import com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest;
import com.codinghappy.fintechai.module.analysis.dto.AnalysisResult;
import com.codinghappy.fintechai.module.analysis.dto.BusinessType;
import com.codinghappy.fintechai.module.analysis.exception.AnalysisException;
import com.codinghappy.fintechai.module.analysis.retry.RetryTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekAnalysisService implements AnalysisService {

    private final ChatClient chatClient;
    private final RateLimitService rateLimitService;
    @Qualifier("customRetryTemplate")
    private final RetryTemplate retryTemplate;
    private final ObjectMapper objectMapper;

    @Value("${finance.analysis.deepseek.prompt-template}")
    private String promptTemplate;

    // 缓存已分析的公司简介，减少API调用[citation:10]
    private final Map<String, AnalysisResult> analysisCache =
            new ConcurrentHashMap<>(1024);

    private static final Set<String> BUSINESS_KEYWORDS = Set.of(
            "跨境支付", "国际支付", "跨境结算", "外汇支付",
            "海外借贷", "国际贷款", "跨境融资", "海外融资"
    );

    /**
     * 分析公司简介
     */
    @Override
    public AnalysisResult analyzeCompany(AnalysisRequest request) {
        String cacheKey = generateCacheKey(request.getDescription());

        // 检查缓存
        AnalysisResult cachedResult = analysisCache.get(cacheKey);
        if (cachedResult != null) {
            log.debug("返回缓存的分析结果，公司: {}", request.getCompanyName());
            return cachedResult;
        }

        // 限流控制[citation:6]
//        if (!rateLimitService.tryAcquire()) {
//            throw new AnalysisException("系统繁忙，请稍后重试");
//        }

        try {
            // 使用重试机制调用DeepSeek API
            AnalysisResult result = retryTemplate.executeWithRetry(() ->
                    callDeepSeekApi(request)
            );

            // 缓存结果
            analysisCache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            log.error("分析公司失败: {}, 错误: {}", request.getCompanyName(), e.getMessage());

            // 降级方案：使用关键词匹配
            return fallbackAnalysis(request.getDescription());
        }
    }

    /**
     * 调用DeepSeek API[citation:4][citation:9]
     */
    private AnalysisResult callDeepSeekApi(AnalysisRequest request) {
        try {
            String prompt = promptTemplate.replace("{description}",
                    request.getDescription());

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("DeepSeek API响应: {}", response);

            return parseApiResponse(response, request.getDescription());

        } catch (Exception e) {
            throw new AnalysisException("DeepSeek API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析DeepSeek API响应
     */
    private AnalysisResult parseApiResponse(String response, String description) {
        try {
            // 提取JSON部分
            String jsonStr = extractJsonFromResponse(response);
            JsonNode jsonNode = objectMapper.readTree(jsonStr);

            AnalysisResult result = new AnalysisResult();

            // 解析业务类型
            List<BusinessType> businessTypes = new ArrayList<>();
            JsonNode typesNode = jsonNode.get("business_types");
            if (typesNode != null && typesNode.isArray()) {
                for (JsonNode typeNode : typesNode) {
                    String type = typeNode.asText();
                    if ("跨境支付".equals(type)) {
                        businessTypes.add(BusinessType.CROSS_BORDER_PAYMENT);
                    } else if ("海外借贷".equals(type)) {
                        businessTypes.add(BusinessType.OVERSEAS_LOAN);
                    }
                }
            }
            result.setBusinessTypes(businessTypes);

            // 解析评分
            JsonNode scoreNode = jsonNode.get("payment_willingness_score");
            if (scoreNode != null) {
                result.setPaymentWillingnessScore(scoreNode.asInt());
            } else {
                result.setPaymentWillingnessScore(5); // 默认值
            }

            // 解析置信度
            JsonNode confidenceNode = jsonNode.get("confidence");
            if (confidenceNode != null) {
                result.setConfidence(confidenceNode.asDouble());
            }

            // 解析理由
            JsonNode reasonNode = jsonNode.get("reason");
            if (reasonNode != null) {
                result.setAnalysisReason(reasonNode.asText());
            }

            result.setRawResponse(response);
            result.setSuccess(true);

            return result;

        } catch (Exception e) {
            log.warn("解析API响应失败，使用降级分析，响应: {}", response);
            return fallbackAnalysis(description);
        }
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        // 查找JSON开始和结束位置
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }

    /**
     * 降级分析：基于关键词的简单分析
     */
    private AnalysisResult fallbackAnalysis(String description) {
        AnalysisResult result = new AnalysisResult();
        List<BusinessType> businessTypes = new ArrayList<>();

        // 关键词匹配
        String lowerDesc = description.toLowerCase();
        for (String keyword : BUSINESS_KEYWORDS) {
            if (lowerDesc.contains(keyword.toLowerCase())) {
                if (keyword.contains("支付")) {
                    businessTypes.add(BusinessType.CROSS_BORDER_PAYMENT);
                } else if (keyword.contains("借贷") || keyword.contains("贷款")) {
                    businessTypes.add(BusinessType.OVERSEAS_LOAN);
                }
            }
        }

        result.setBusinessTypes(businessTypes);
        result.setPaymentWillingnessScore(calculateFallbackScore(description));
        result.setConfidence(0.6);
        result.setAnalysisReason("基于关键词匹配的降级分析");
        result.setSuccess(true);

        return result;
    }

    /**
     * 计算降级评分
     */
    private int calculateFallbackScore(String description) {
        int score = 5; // 基础分

        // 根据关键词数量调整分数
        long keywordCount = BUSINESS_KEYWORDS.stream()
                .filter(keyword -> description.toLowerCase()
                        .contains(keyword.toLowerCase()))
                .count();

        score += Math.min(keywordCount, 3); // 每匹配一个关键词加1分，最多加3分

        // 根据描述长度调整
        if (description.length() > 200) {
            score += 1; // 详细描述加1分
        }

        return Math.min(score, 10); // 确保不超过10分
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String description) {
        return Integer.toHexString(description.hashCode());
    }

    /**
     * 批量分析
     */
    @Override
    public List<AnalysisResult> batchAnalyze(List<AnalysisRequest> requests) {
        List<AnalysisResult> results = new ArrayList<>(requests.size());

        // 使用普通的 for 循环，方便处理异常和休眠
        for (AnalysisRequest request : requests) {
            try {
                // --- 核心修复：手动降频 ---
                // 每次分析前强制休息 1.5 秒，确保完全绕过本地限流和 DeepSeek 的频率限制
                Thread.sleep(1500);

                log.info("正在分析公司: {}", request.getCompanyName());
                AnalysisResult result = analyzeCompany(request);
                results.add(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("分析失败，公司: {}", request.getCompanyName(), e);
                results.add(AnalysisResult.errorResult(e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        analysisCache.clear();
        log.info("已清空分析缓存");
    }
}