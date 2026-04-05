package com.example.aiframework.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 任务日志
 */
public class AgentTaskService_TaskLog {
    private String logType;
    private String content;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    public AgentTaskService_TaskLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
