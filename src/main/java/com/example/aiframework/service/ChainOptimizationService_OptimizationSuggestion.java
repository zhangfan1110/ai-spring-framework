package com.example.aiframework.service;

/**
 * 调用链优化建议
 */
public class ChainOptimizationService_OptimizationSuggestion {
    private String type; // PERFORMANCE/RELIABILITY/COST/COMPLEXITY
    private String severity; // HIGH/MEDIUM/LOW
    private String description;
    private String suggestion;
    private int estimatedImprovement; // 百分比
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    
    public int getEstimatedImprovement() { return estimatedImprovement; }
    public void setEstimatedImprovement(int estimatedImprovement) { this.estimatedImprovement = estimatedImprovement; }
}
