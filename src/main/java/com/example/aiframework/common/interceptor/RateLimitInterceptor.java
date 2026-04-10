package com.example.aiframework.common.interceptor;

import com.example.aiframework.common.annotation.LimitType;
import com.example.aiframework.common.annotation.RateLimit;
import com.example.aiframework.system.service.RateLimitService;
import com.example.aiframework.common.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 限流拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查方法或类上是否有 @RateLimit 注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null) {
            return true;
        }

        // 生成限流 key
        String limitKey = generateLimitKey(request, rateLimit);

        // 尝试获取令牌
        boolean allowed = rateLimitService.tryAcquire(
            limitKey,
            rateLimit.maxRequests(),
            rateLimit.time(),
            rateLimit.timeUnit()
        );

        if (!allowed) {
            // 返回 429 Too Many Requests
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            
            Result<?> result = Result.error(rateLimit.message());
            response.getWriter().write(objectMapper.writeValueAsString(result));
            
            return false;
        }

        // 将剩余次数添加到响应头
        Long remaining = rateLimitService.getRemainingRequests(limitKey, rateLimit.maxRequests());
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.maxRequests()));

        return true;
    }

    /**
     * 生成限流 key
     */
    private String generateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加接口路径
        String apiPath = request.getRequestURI();
        keyBuilder.append("api:").append(apiPath.replace("/", ":"));
        
        // 根据限流类型添加标识
        switch (rateLimit.limitType()) {
            case IP:
                String ip = getClientIp(request);
                keyBuilder.append(":ip:").append(ip);
                break;
                
            case USER:
                String userId = getCurrentUserId();
                if (StringUtils.hasText(userId)) {
                    keyBuilder.append(":user:").append(userId);
                } else {
                    // 未登录用户使用 IP
                    keyBuilder.append(":ip:").append(getClientIp(request));
                }
                break;
                
            case GLOBAL:
                keyBuilder.append(":global");
                break;
                
            case API:
                // 仅按接口限流，不区分用户
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多个 IP 时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取当前用户 ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    return ((org.springframework.security.core.userdetails.User) principal).getUsername();
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回 null
        }
        return null;
    }
}
