package com.example.aiframework.service;

import java.util.Map;

/**
 * 工具基准测试报告
 */
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
    
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }
    
    public long getTotalTime() { return totalTime; }
    public void setTotalTime(long totalTime) { this.totalTime = totalTime; }
    
    public long getAvgTime() { return avgTime; }
    public void setAvgTime(long avgTime) { this.avgTime = avgTime; }
    
    public long getMinTime() { return minTime; }
    public void setMinTime(long minTime) { this.minTime = minTime; }
    
    public long getMaxTime() { return maxTime; }
    public void setMaxTime(long maxTime) { this.maxTime = maxTime; }
    
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
