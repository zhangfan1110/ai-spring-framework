package com.example.aiframework.chat.model;

/**
 * 聊天请求
 */
public class ChatRequest {
    
    private String sessionId;
    private String message;
    private String systemPrompt;
    private String model;
    private Double temperature;
    
    public ChatRequest() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
