package com.example.aiframework.dto;

/**
 * ReAct 执行请求
 */
public class ReActRequest {
    
    private String task;
    private Integer maxIterations;
    
    public ReActRequest() {}
    
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    
    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }
}
