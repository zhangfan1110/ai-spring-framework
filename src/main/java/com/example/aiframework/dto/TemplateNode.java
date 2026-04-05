package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 模板节点
 */
@Setter
@Getter
public class TemplateNode {
    private String stepId;
    private String toolName;
    private String description;
    private Map<String, String> parameters;
    private String dependsOn;
    private boolean parallel;
    private int sortOrder;

}
