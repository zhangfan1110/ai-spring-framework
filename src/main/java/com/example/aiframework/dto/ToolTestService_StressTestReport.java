package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工具压力测试报告
 */
@Setter
@Getter
public class ToolTestService_StressTestReport {
    private String toolName;
    private int concurrentUsers;
    private int totalRequests;
    private int successRequests;
    private int failedRequests;
    private double requestsPerSecond;
    private long avgResponseTime;
    private long p95ResponseTime;
    private long p99ResponseTime;
    private String status; // PASS/FAIL
    private List<String> errors;

}
