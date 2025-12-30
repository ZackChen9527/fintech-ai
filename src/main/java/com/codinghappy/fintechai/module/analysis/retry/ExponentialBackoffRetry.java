package com.codinghappy.fintechai.module.analysis.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Slf4j
@Component
public class ExponentialBackoffRetry {

    @Value("${finance.analysis.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${finance.analysis.retry.initial-interval:1000}")
    private long initialInterval;

    @Value("${finance.analysis.retry.multiplier:2.0}")
    private double multiplier;

    /**
     * 执行带指数退避的重试
     */
    public <T> T execute(RetryableOperation<T> operation, Predicate<Exception> retryPredicate) {
        int attempt = 0;
        long interval = initialInterval;

        while (attempt < maxAttempts) {
            attempt++;

            try {
                return operation.execute();

            } catch (Exception e) {
                log.warn("操作失败，尝试次数: {}, 错误: {}", attempt, e.getMessage());

                // 检查是否应该重试
                if (!retryPredicate.test(e) || attempt >= maxAttempts) {
                    log.error("达到最大重试次数或不可重试错误，放弃重试", e);
                    try {
                        throw e;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                // 指数退避等待
                try {
                    log.info("等待 {}ms 后重试", interval);
                    TimeUnit.MILLISECONDS.sleep(interval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }

                // 增加等待时间
                interval = (long) (interval * multiplier);
            }
        }

        throw new IllegalStateException("不应到达此处");
    }

    /**
     * 执行重试（默认重试所有异常）
     */
    public <T> T execute(RetryableOperation<T> operation) {
        return execute(operation, e -> true);
    }

    /**
     * 可重试操作接口
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}