package com.example.aiframework.service;

import java.util.Map;

/**
 * 工具测试结果
 */
public class ToolTestService_TestResult {
    private String toolName;
    private boolean success;
    private String message;
    private Object output;
    private long duration;
    private Map<String, Object> metrics;
    
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Object getOutput() { return output; }
    public void setOutput(Object output) { this.output = output; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
}
