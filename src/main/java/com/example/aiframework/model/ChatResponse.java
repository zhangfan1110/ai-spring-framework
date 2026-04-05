package com.example.aiframework.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 聊天响应
 */
public class ChatResponse {
    
    private String sessionId;
    private String content;
    private String model;
    private Long duration;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime timestamp;
    
    public ChatResponse() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final ChatResponse response = new ChatResponse();
        
        public Builder sessionId(String sessionId) { response.setSessionId(sessionId); return this; }
        public Builder content(String content) { response.setContent(content); return this; }
        public Builder model(String model) { response.setModel(model); return this; }
        public Builder duration(Long duration) { response.setDuration(duration); return this; }
        public Builder timestamp(LocalDateTime timestamp) { response.setTimestamp(timestamp); return this; }
        
        public ChatResponse build() { return response; }
    }
    
    // Getter for Builder
    public Builder toBuilder() {
        Builder builder = new Builder();
        return builder.sessionId(this.sessionId)
                     .content(this.content)
                     .model(this.model)
                     .duration(this.duration)
                     .timestamp(this.timestamp);
    }
}
