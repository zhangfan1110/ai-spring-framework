package com.example.aiframework.agent.dto;

import java.util.List;

/**
 * 调用链模板
 */
public class ChainTemplate {
    private String id;
    private String name;
    private String description;
    private String category;
    private int estimatedDuration;
    private int difficulty;
    private List<TemplateNode> nodes;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public List<TemplateNode> getNodes() { return nodes; }
    public void setNodes(List<TemplateNode> nodes) { this.nodes = nodes; }
}
