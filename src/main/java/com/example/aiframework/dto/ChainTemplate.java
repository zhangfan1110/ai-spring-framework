package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 调用链模板
 */
@Setter
@Getter
public class ChainTemplate {
    private String id;
    private String name;
    private String description;
    private String category;
    private int estimatedDuration;
    private int difficulty;
    private List<TemplateNode> nodes;

}
