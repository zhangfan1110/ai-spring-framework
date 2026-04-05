package com.example.aiframework.service;

import java.util.List;

/**
 * 工具压力测试报告
 */
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
    
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public int getConcurrentUsers() { return concurrentUsers; }
    public void setConcurrentUsers(int concurrentUsers) { this.concurrentUsers = concurrentUsers; }
    
    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
    
    public int getSuccessRequests() { return successRequests; }
    public void setSuccessRequests(int successRequests) { this.successRequests = successRequests; }
    
    public int getFailedRequests() { return failedRequests; }
    public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }
    
    public double getRequestsPerSecond() { return requestsPerSecond; }
    public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
    
    public long getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(long avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    
    public long getP95ResponseTime() { return p95ResponseTime; }
    public void setP95ResponseTime(long p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
    
    public long getP99ResponseTime() { return p99ResponseTime; }
    public void setP99ResponseTime(long p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
