package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 工具基准测试报告
 */
@Setter
@Getter
public class ToolTestService_BenchmarkReport {
    private String toolName;
    private int iterations;
    private long totalTime;
    private long avgTime;
    private long minTime;
    private long maxTime;
    private int successCount;
    private int failCount;
    private double successRate;
    private Map<String, Object> details;

}
