package com.example.aiframework.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 对话消息
 */
public class ReActContextService_ConversationMessage {
    private String role; // USER/AI/SYSTEM
    private String content;
    private long timestamp;
    private Map<String, Object> metadata;
    
    public ReActContextService_ConversationMessage() {
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }
    
    public ReActContextService_ConversationMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
