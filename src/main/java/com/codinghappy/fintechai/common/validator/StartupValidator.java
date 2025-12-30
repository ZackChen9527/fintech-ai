package com.codinghappy.fintechai.common.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

@Slf4j
@Component
public class StartupValidator implements CommandLineRunner {

    private final Environment environment;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public StartupValidator(Environment environment,
                            DataSource dataSource,
                            RedisConnectionFactory redisConnectionFactory) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 开始系统启动验证 ===");

        // 1. 检查运行环境
        checkEnvironment();

        // 2. 检查数据库连接
        checkDatabaseConnection();

        // 3. 检查Redis连接
        checkRedisConnection();

        // 4. 检查配置文件
        checkConfiguration();

        log.info("=== 系统启动验证完成 ===");
    }

    private void checkEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("当前激活的Profile: {}", Arrays.toString(activeProfiles));

        String appName = environment.getProperty("spring.application.name");
        String appVersion = environment.getProperty("app.version", "1.0.0");

        log.info("应用名称: {}", appName);
        log.info("应用版本: {}", appVersion);
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {} {}",
                System.getProperty("os.name"),
                System.getProperty("os.version"));
    }

    private void checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                String url = connection.getMetaData().getURL();
                String dbName = connection.getCatalog();
                log.info("数据库连接正常: {}, 数据库: {}", url, dbName);
            } else {
                log.error("数据库连接无效");
            }
        } catch (SQLException e) {
            log.error("数据库连接失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    private void checkRedisConnection() {
        try {
            redisConnectionFactory.getConnection().ping();
            log.info("Redis连接正常");
        } catch (Exception e) {
            log.error("Redis连接失败: {}", e.getMessage());
            // Redis不是必须的，只记录警告
            log.warn("Redis连接失败，系统将使用降级方案");
        }
    }

    private void checkConfiguration() {
        // 检查必要的配置项
        String[] requiredConfigs = {
                "spring.ai.openai.api-key",
                "spring.datasource.url",
                "spring.datasource.username"
        };

        for (String configKey : requiredConfigs) {
            String value = environment.getProperty(configKey);
            if (value == null || value.trim().isEmpty()) {
                log.warn("配置项 {} 未设置或为空", configKey);
            } else {
                log.debug("配置项 {}: {}", configKey,
                        configKey.contains("password") || configKey.contains("key")
                                ? "***" : value);
            }
        }

        // 检查DeepSeek API配置
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (apiKey == null || apiKey.trim().isEmpty() ||
                apiKey.contains("your-deepseek-api-key")) {
            log.warn("DeepSeek API密钥未配置，AI分析功能将使用模拟模式");
        } else {
            log.info("DeepSeek API配置正常");
        }

        // 检查限流配置
        String rateLimitTokens = environment.getProperty(
                "finance.analysis.rate-limit.tokens-per-second", "5");
        log.info("API限流配置: {} tokens/秒", rateLimitTokens);

        // 检查任务调度配置
        String analysisCron = environment.getProperty(
                "finance.scheduler.analysis-batch.cron", "0 */30 * * * ?");
        log.info("批量分析任务调度: {}", analysisCron);
    }
}