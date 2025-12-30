package com.codinghappy.fintechai.module.analysis.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component("customRetryTemplate")
@RequiredArgsConstructor
public class RetryTemplate {

    private final ExponentialBackoffRetry exponentialBackoffRetry;

    /**
     * 执行带重试的操作
     */
    public <T> T executeWithRetry(ExponentialBackoffRetry.RetryableOperation<T> operation) {
        // 只重试RuntimeException
        Predicate<Exception> retryPredicate = e -> e instanceof RuntimeException;

        return exponentialBackoffRetry.execute(operation, retryPredicate);
    }

    /**
     * 执行带重试的操作（自定义重试条件）
     */
    public <T> T executeWithRetry(ExponentialBackoffRetry.RetryableOperation<T> operation,
                                  Predicate<Exception> retryPredicate) {
        return exponentialBackoffRetry.execute(operation, retryPredicate);
    }
}