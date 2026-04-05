package com.example.aiframework.dto;

/**
 * 会话信息
 */
public class ChatSessionExportDTO_SessionInfo {
    private String sessionId;
    private String title;
    private Integer messageCount;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime lastActiveTime;
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(java.time.LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
}
