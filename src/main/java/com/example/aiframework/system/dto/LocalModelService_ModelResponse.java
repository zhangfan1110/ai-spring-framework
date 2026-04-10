package com.example.aiframework.system.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地模型响应
 */
public class LocalModelService_ModelResponse {
    private String content;
    private String model;
    private long duration;
    private Map<String, Object> metadata;

    public LocalModelService_ModelResponse() {
        this.metadata = new HashMap<>();
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
