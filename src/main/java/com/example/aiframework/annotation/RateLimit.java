package com.example.aiframework.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * API 限流注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 key，支持 SpEL 表达式
     * 默认使用接口路径
     */
    String key() default "";

    /**
     * 限流时间窗口
     */
    int time() default 60;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 时间窗口内最大请求数
     */
    int maxRequests() default 100;

    /**
     * 限流类型：IP/USER/GLOBAL
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
