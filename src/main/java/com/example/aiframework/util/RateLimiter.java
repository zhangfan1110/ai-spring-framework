package com.example.aiframework.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 简单的限流器 (基于令牌桶算法)
 */
public class RateLimiter {
    
    private final int maxRequests;
    private final long timeWindow;
    private final TimeUnit timeUnit;
    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();
    
    public RateLimiter(int maxRequests, long timeWindow, TimeUnit timeUnit) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.timeUnit = timeUnit;
    }
    
    /**
     * 检查是否允许请求
     */
    public boolean allowRequest(String key) {
        long now = System.currentTimeMillis();
        long windowMillis = timeUnit.toMillis(timeWindow);
        
        RequestCount count = requestCounts.computeIfAbsent(key, k -> new RequestCount());
        
        synchronized (count) {
            // 清理过期窗口
            if (now - count.windowStart > windowMillis) {
                count.count = 0;
                count.windowStart = now;
            }
            
            // 检查是否超过限制
            if (count.count >= maxRequests) {
                return false;
            }
            
            // 增加计数
            count.count++;
            return true;
        }
    }
    
    /**
     * 获取剩余请求数
     */
    public int getRemainingRequests(String key) {
        long now = System.currentTimeMillis();
        long windowMillis = timeUnit.toMillis(timeWindow);
        
        RequestCount count = requestCounts.get(key);
        if (count == null) {
            return maxRequests;
        }
        
        synchronized (count) {
            if (now - count.windowStart > windowMillis) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - count.count);
        }
    }
    
    /**
     * 清理过期数据
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        long windowMillis = timeUnit.toMillis(timeWindow);
        
        requestCounts.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > windowMillis * 2
        );
    }
    
    private static class RequestCount {
        int count = 0;
        long windowStart = System.currentTimeMillis();
    }
}
