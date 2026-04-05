package com.example.aiframework.interceptor;

import com.example.aiframework.exception.BusinessException;
import com.example.aiframework.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private final RateLimiter rateLimiter;
    
    public RateLimitInterceptor() {
        // 每分钟最多 60 次请求
        this.rateLimiter = new RateLimiter(60, 1, TimeUnit.MINUTES);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        
        // 健康检查不限流
        if (uri.contains("/health")) {
            return true;
        }
        
        // 限流 key: IP + URI
        String key = clientIp + ":" + uri;
        
        if (!rateLimiter.allowRequest(key)) {
            int remaining = rateLimiter.getRemainingRequests(key);
            log.warn("限流触发 - IP: {}, URI: {}, 剩余：{}", clientIp, uri, remaining);
            throw new BusinessException(429, "请求过于频繁，请稍后再试 (剩余：" + remaining + ")");
        }
        
        return true;
    }
    
    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
