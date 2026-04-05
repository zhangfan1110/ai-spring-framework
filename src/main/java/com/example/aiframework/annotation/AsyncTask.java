package com.example.aiframework.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncTask {

    /**
     * 任务类型
     */
    String type() default "";

    /**
     * 任务队列名称
     */
    String queue() default "default";

    /**
     * 延迟执行时间
     */
    long delay() default 0;

    /**
     * 延迟时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 最大重试次数
     */
    int maxRetry() default 3;

    /**
     * 超时时间（秒）
     */
    long timeout() default 300;

    /**
     * 优先级：1-最高 5-最低
     */
    int priority() default 3;
}
