package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 对话消息
 */
@Setter
@Getter
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

}
