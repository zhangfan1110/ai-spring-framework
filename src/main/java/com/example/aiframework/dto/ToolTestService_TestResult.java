package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 工具测试结果
 */
@Setter
@Getter
public class ToolTestService_TestResult {
    private String toolName;
    private boolean success;
    private String message;
    private Object output;
    private long duration;
    private Map<String, Object> metrics;

}
