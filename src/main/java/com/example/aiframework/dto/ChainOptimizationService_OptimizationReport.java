package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 调用链优化分析报告
 */
@Setter
@Getter
public class ChainOptimizationService_OptimizationReport {
    private int originalSteps;
    private int optimizedSteps;
    private int originalDuration;
    private int optimizedDuration;
    private List<ChainOptimizationService_OptimizationSuggestion> suggestions;
    private String summary;
    private Map<String, Object> details;

}
