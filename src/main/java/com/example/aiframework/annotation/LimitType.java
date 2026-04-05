package com.example.aiframework.annotation;

/**
 * 限流类型
 */
public enum LimitType {
    /**
     * 按 IP 限流
     */
    IP,
    
    /**
     * 按用户限流
     */
    USER,
    
    /**
     * 全局限流
     */
    GLOBAL,
    
    /**
     * 按接口限流
     */
    API
}
