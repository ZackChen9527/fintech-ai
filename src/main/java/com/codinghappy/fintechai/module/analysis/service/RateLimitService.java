package com.codinghappy.fintechai.module.analysis.service;

import com.codinghappy.fintechai.module.analysis.exception.RateLimitException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Semaphore localSemaphore;

    @Value("${finance.analysis.rate-limit.tokens-per-second:5}")
    private double tokensPerSecond;

    @Value("${finance.analysis.rate-limit.burst-capacity:10}")
    private double burstCapacity;

    @Value("${finance.analysis.rate-limit.timeout-millis:1000}")
    private long timeoutMillis;

    // Lua脚本实现令牌桶算法[citation:6]
    private static final String TOKEN_BUCKET_LUA = """
        local key = KEYS[1]
        local tokensPerSecond = tonumber(ARGV[1])
        local burstCapacity = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])
        
        local bucket = redis.call('hmget', key, 'tokens', 'timestamp')
        local tokens = burstCapacity
        local lastTime = now
        
        if bucket[1] and bucket[2] then
            tokens = tonumber(bucket[1])
            lastTime = tonumber(bucket[2])
        end
        
        -- 计算新增的令牌
        local timePassed = now - lastTime
        local newTokens = timePassed * tokensPerSecond
        tokens = math.min(tokens + newTokens, burstCapacity)
        
        -- 检查是否有足够令牌
        if tokens >= requested then
            tokens = tokens - requested
            redis.call('hmset', key, 'tokens', tokens, 'timestamp', now)
            redis.call('expire', key, 3600)
            return 1
        else
            redis.call('hmset', key, 'tokens', tokens, 'timestamp', now)
            redis.call('expire', key, 3600)
            return 0
        end
        """;

    private DefaultRedisScript<Long> redisScript;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 本地信号量作为第二道防线
        this.localSemaphore = new Semaphore((int) burstCapacity);
    }

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(TOKEN_BUCKET_LUA);
        redisScript.setResultType(Long.class);
    }

    /**
     * 尝试获取调用许可
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * 尝试获取指定数量的调用许可
     */
    public boolean tryAcquire(int permits) {
        // 第一层：本地信号量限流
        if (!localSemaphore.tryAcquire(permits)) {
            log.warn("本地限流触发，当前信号量: {}", localSemaphore.availablePermits());
            return false;
        }

        try {
            // 第二层：Redis分布式限流
            String key = "rate_limit:deepseek:" + Instant.now().getEpochSecond() / 60;
            List<String> keys = Arrays.asList(key);

            Long result = redisTemplate.execute(
                    redisScript,
                    keys,
                    String.valueOf(tokensPerSecond),
                    String.valueOf(burstCapacity),
                    String.valueOf(Instant.now().getEpochSecond()),
                    String.valueOf(permits)
            );

            boolean allowed = result != null && result == 1;

            if (!allowed) {
                log.warn("分布式限流触发，key: {}", key);
                localSemaphore.release(permits); // 释放本地信号量
            }

            return allowed;

        } catch (Exception e) {
            log.error("限流检查异常，放行请求", e);
            // Redis异常时放行，避免系统不可用
            return true;
        }
    }

    /**
     * 等待获取调用许可
     */
    public void acquire() throws InterruptedException {
        if (!tryAcquireWithTimeout()) {
            throw new RateLimitException("获取API调用许可超时");
        }
    }

    /**
     * 带超时的尝试获取
     */
    private boolean tryAcquireWithTimeout() throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(100); // 等待100ms再试
        }

        return false;
    }

    /**
     * 释放许可
     */
    public void release() {
        localSemaphore.release();
    }

    /**
     * 获取当前限流状态
     */
    public RateLimitStatus getStatus() {
        return new RateLimitStatus(
                localSemaphore.availablePermits(),
                tokensPerSecond,
                burstCapacity
        );
    }

    public record RateLimitStatus(
            int availablePermits,
            double tokensPerSecond,
            double burstCapacity
    ) {}
}