package com.example.aiframework.dto;

/**
 * Ollama 聊天消息
 */
public class LocalModelService_OllamaChatRequest_Message {
    private String role;
    private String content;
    
    public LocalModelService_OllamaChatRequest_Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
