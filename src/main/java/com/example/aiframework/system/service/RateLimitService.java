package com.example.aiframework.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务（基于 Redis 令牌桶）
 */
@Service
public class RateLimitService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate:limit:";

    /**
     * 检查是否允许请求
     * 
     * @param key 限流 key
     * @param maxRequests 最大请求数
     * @param timeWindow 时间窗口
     * @param timeUnit 时间单位
     * @return true-允许，false-拒绝
     */
    public boolean tryAcquire(String key, int maxRequests, long timeWindow, TimeUnit timeUnit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        // 获取当前计数
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count == null) {
            return false;
        }
        
        // 如果是第一次请求，设置过期时间
        if (count == 1) {
            redisTemplate.expire(redisKey, timeWindow, timeUnit);
            return true;
        }
        
        // 检查是否超过限制
        return count <= maxRequests;
    }

    /**
     * 获取剩余请求次数
     */
    public Long getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long count = (Long) redisTemplate.opsForValue().get(redisKey);
        
        if (count == null) {
            return (long) maxRequests;
        }
        
        return Math.max(0, maxRequests - count);
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        redisTemplate.delete(redisKey);
    }

    /**
     * 生成限流 key
     */
    public String generateKey(String prefix, String identifier) {
        return prefix + ":" + identifier;
    }
}
