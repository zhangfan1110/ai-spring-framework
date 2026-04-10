package com.example.aiframework.chat.dto;

import java.time.LocalDateTime;

/**
 * 会话信息
 */
public class ChatSessionExportDTO_SessionInfo {
    private String sessionId;
    private String title;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime lastActiveTime;
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
}
