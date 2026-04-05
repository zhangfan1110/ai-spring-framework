package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 调用链优化建议
 */
@Setter
@Getter
public class ChainOptimizationService_OptimizationSuggestion {
    private String type; // PERFORMANCE/RELIABILITY/COST/COMPLEXITY
    private String severity; // HIGH/MEDIUM/LOW
    private String description;
    private String suggestion;
    private int estimatedImprovement; // 百分比

}
