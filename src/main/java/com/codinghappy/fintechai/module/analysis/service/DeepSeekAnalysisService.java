package com.codinghappy.fintechai.module.analysis.service; // ⚠️ 确认你的包名

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codinghappy.fintechai.repository.AnalysisResultRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekAnalysisService {

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String apiUrl;

    public AnalysisResultEntity analyzeCompany(Long companyId, String companyName, String description) {
        log.info(">>> 开始分析公司: {} (ID: {})", companyName, companyId);
        long startTime = System.currentTimeMillis();

        AnalysisResultEntity.AnalysisResultEntityBuilder resultBuilder = AnalysisResultEntity.builder()
                .companyId(companyId)
                .analysisModel("deepseek-chat-v3")
                .version(3)
                .analysisTime(LocalDateTime.now());

        try {
            String prompt = buildCommercialSpyPrompt(companyName, description);
            String rawResponse = callDeepSeekApi(prompt);
            long duration = System.currentTimeMillis() - startTime;

            // 🔥 核心修复：解析 OpenAI 格式的响应
            JSONObject aiData = parseAiResponse(rawResponse);

            if (aiData == null) {
                // 如果解析失败，抛异常，让外层重试或记录
                throw new RuntimeException("无法从AI响应中提取有效JSON");
            }

            // 组装成好看的报告
            String commercialReport = generateCommercialReport(aiData);

            AnalysisResultEntity entity = resultBuilder
                    .success(true)
                    .rawResponse(rawResponse)
                    .processingTimeMs((double) duration)
                    .analysisReason(commercialReport) // 这里现在肯定有值了！
                    .businessTypes(aiData.getString("business_category"))
                    .paymentWillingnessScore(aiData.getInteger("score"))
                    .confidence(aiData.getDouble("confidence"))
                    .build();

            return analysisResultRepository.save(entity);

        } catch (Exception e) {
            log.error(">>> 分析失败: {}", companyName, e);
            AnalysisResultEntity errorEntity = resultBuilder
                    .success(false)
                    .errorMessage(e.getMessage())
                    .processingTimeMs((double) (System.currentTimeMillis() - startTime))
                    .build();
            analysisResultRepository.save(errorEntity);
            throw new RuntimeException("分析失败: " + e.getMessage());
        }
    }

    public List<AnalysisResultEntity> batchAnalyze(List<com.codinghappy.fintechai.module.analysis.dto.AnalysisRequest> requests) {
        List<AnalysisResultEntity> results = new ArrayList<>();
        for (var req : requests) {
            try {
                results.add(analyzeCompany(req.getCompanyId(), req.getCompanyName(), req.getDescription()));
            } catch (Exception e) { /* ignore */ }
        }
        return results;
    }

    // --- 私有辅助方法 ---

    private String buildCommercialSpyPrompt(String name, String desc) {
        return "你是一名拥有10年经验的Fintech行业销售总监。请分析以下目标公司的信息，为我挖掘销售线索。\n\n" +
                "【目标公司】: " + name + "\n" +
                "【公司简介】: " + desc + "\n\n" +
                "请务必严格按照以下 JSON 格式输出结果（不要输出 markdown 代码块，只输出纯文本 JSON）：\n" +
                "{\n" +
                "  \"business_category\": \"用3-5个字精准定义其业务(如:跨境支付/Web3钱包)\",\n" +
                "  \"pain_points\": [\"痛点1: 描述具体的技术或合规难题\", \"痛点2\", \"痛点3\"],\n" +
                "  \"score\": 1-10的整数(代表付费意愿),\n" +
                "  \"confidence\": 0.0-1.0(代表你的判断置信度),\n" +
                "  \"sales_hook\": \"一句为销售量身定制的破冰开场白(中文)\",\n" +
                "  \"value_summary\": \"简述为什么这家公司值得跟进(50字以内)\"\n" +
                "}";
    }

    private String generateCommercialReport(JSONObject data) {
        StringBuilder sb = new StringBuilder();
        // 增加空值判断，防止 NullPointerException
        String category = data.getString("business_category");
        sb.append("【业务本质】: ").append(category != null ? category : "未识别").append("\n\n");

        sb.append("【核心痛点预测】:\n");
        JSONArray painPoints = data.getJSONArray("pain_points");
        if (painPoints != null) {
            for (int i = 0; i < painPoints.size(); i++) {
                sb.append(i + 1).append(". ").append(painPoints.getString(i)).append("\n");
            }
        }

        sb.append("\n【销售敲门砖】:\n\"").append(data.getString("sales_hook")).append("\"\n\n");
        sb.append("【深度价值评估】:\n").append(data.getString("value_summary"));
        return sb.toString();
    }

    // 🔥 修复后的解析逻辑
    private JSONObject parseAiResponse(String rawResponse) {
        try {
            JSONObject root = JSON.parseObject(rawResponse);

            // 优先检查 OpenAI 格式 (choices -> message -> content)
            if (root.containsKey("choices")) {
                JSONArray choices = root.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    String content = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    return parseCleanJson(content);
                }
            }

            // 否则尝试直接解析 root (防止 DeepSeek 改格式)
            return root;

        } catch (Exception e) {
            // 最后的兜底：把它当纯文本处理
            return parseCleanJson(rawResponse);
        }
    }

    private JSONObject parseCleanJson(String content) {
        if (content == null) return null;
        try {
            // 去掉 markdown 的 ```json 和 ``` 包裹
            String clean = content.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            return JSON.parseObject(clean);
        } catch (Exception e) {
            log.error("JSON清洗失败，内容: {}", content);
            return null;
        }
    }

    private String callDeepSeekApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", new Object[]{message});
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
        return response.getBody();
    }
}