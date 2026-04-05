package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Ollama 聊天消息
 */
@Setter
@Getter
public class LocalModelService_OllamaChatRequest_Message {
    private String role;
    private String content;
    
    public LocalModelService_OllamaChatRequest_Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

}
